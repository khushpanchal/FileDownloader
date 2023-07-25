package com.khush.workmanager.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.khush.workmanager.bean.DownloadItem
import com.khush.workmanager.util.Const.BLOCKED
import com.khush.workmanager.util.Const.CANCELLED
import com.khush.workmanager.util.Const.FAILED
import com.khush.workmanager.util.Const.FILENAME
import com.khush.workmanager.util.Const.KEY_URL
import com.khush.workmanager.util.Const.QUEUED
import com.khush.workmanager.util.Const.RUNNING
import com.khush.workmanager.util.Const.SUCCESS
import com.khush.workmanager.util.Const.TAG_DOWNLOAD
import com.khush.workmanager.util.Const.URL
import com.khush.workmanager.worker.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val application: Application): AndroidViewModel(application) {

    private val downloadItemsLiveData = MutableLiveData<List<DownloadItem>>()
    fun getDownloadItemsLiveData(): LiveData<List<DownloadItem>> {
        return downloadItemsLiveData
    }

    private val downloadItems = mutableListOf<DownloadItem>()

    private val workManager = WorkManager.getInstance(application)

    init {
        //exsisting work info query
        viewModelScope.launch(Dispatchers.IO) {
            val workInfosLiveData: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData(TAG_DOWNLOAD)
            val transformedLiveData: LiveData<List<DownloadItem>> = Transformations.map(workInfosLiveData) { workInfos ->
                downloadItems.clear()
                for (workInfo in workInfos) {
                    val status = when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> QUEUED
                        WorkInfo.State.RUNNING -> RUNNING
                        WorkInfo.State.SUCCEEDED -> SUCCESS
                        WorkInfo.State.FAILED -> FAILED
                        WorkInfo.State.CANCELLED -> CANCELLED
                        WorkInfo.State.BLOCKED -> BLOCKED
                    }
                    val fileName = workInfo.outputData.getString(FILENAME)
                        ?: ("Unknown_" + System.currentTimeMillis().toString())
                    if(status == SUCCESS) continue //downloaded file will be queued from file system

                    downloadItems.add(DownloadItem(fileName, status, uuid = workInfo.id, url = workInfo.progress.getString(URL)))
                }
                downloadItems
            }
            withContext(Dispatchers.Main) {
                transformedLiveData.observeForever {
                    downloadItemsLiveData.postValue(syncWithFileSys(it.toMutableList()))
                }
            }
        }
    }

    private fun syncWithFileSys(items: MutableList<DownloadItem>): List<DownloadItem> {
        //get list from file system by time sort and add it below current items
        val files =  application.cacheDir?.listFiles()
        files?.reverse()
        if (files != null) {
            for(file in files) {
                if(file.isFile) {
                    val uri = FileProvider.getUriForFile(
                        application,
                        application.packageName + ".provider",
                        file
                    )
                    items.add(DownloadItem(file.name, SUCCESS, uri))
                }
            }
        }
        return items
    }

    fun startDownloading(url: String?) {
        if(url.isNullOrEmpty()) {
            Toast.makeText(application, "Please enter the url", Toast.LENGTH_SHORT).show()
            return
        }

        val inputData = Data.Builder()
            .putString(KEY_URL, url)
            .build()

        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .addTag(TAG_DOWNLOAD)
            .build()

        workManager.enqueueUniqueWork(
            url, ExistingWorkPolicy.KEEP,
            downloadWorkRequest
        )
    }

    fun cancelStartWork(item: DownloadItem) {
        if(item.status == CANCELLED) {
            if(item.url != null) startDownloading(item.url)
        } else item.uuid?.let { workManager.cancelWorkById(it) }
    }

}