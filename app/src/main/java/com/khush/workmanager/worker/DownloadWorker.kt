package com.khush.workmanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.khush.workmanager.util.Const.FILENAME
import com.khush.workmanager.util.Const.KEY_URL
import com.khush.workmanager.util.Const.REASON
import com.khush.workmanager.util.Const.URL
import com.khush.workmanager.util.DownloadInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloadWorker(private val context: Context, private val workerParameters: WorkerParameters):
    CoroutineWorker(context, workerParameters) {

    // Testing URL:-> https://picsum.photos/200
    //https://fastly.picsum.photos/id/498/200/200.jpg?hmac=qTtPQUlxKQaUfu86iZS07VWbb5NrV6vbPnPq8SM2WC8
    //https://fastly.picsum.photos/id/133/200/200.jpg?hmac=iMRNc08s10YIUOgb0oqyc8fDo7tiPCj86Au7eFqs6Js
    //https://fastly.picsum.photos/id/886/200/200.jpg?hmac=pfmGQi7EpajLoJI0tKTPTUwOPQtH9YwE-wNl_kr7ErI

    override suspend fun doWork(): Result {
//        delay(5000)
        val url = inputData.getString(KEY_URL)
        setProgressAsync(workDataOf(URL to url)) //unique workName, used when restart cancelled work
        if(url.isNullOrEmpty()) {
            return Result.failure(
                workDataOf(
                    REASON to "Invalid URL",
                    FILENAME to "Unknown_"+System.currentTimeMillis().toString()
                )
            )
        }
//        setProgress(workDataOf(FILENAME to "Unknown"))
        val response = DownloadInterface.instance.downloadFile(url)
        response.body()?.let {body ->
            return withContext(Dispatchers.IO) {
                val fileName = "File_"+ (response.body()!!.contentType()?.type() ?:"")+"_"+System.currentTimeMillis().toString()+"."+(response.body()!!.contentType()?.subtype() ?: "")
//                setProgress(workDataOf(FILENAME to fileName, "percent" to "0"))
                val file = File(context.cacheDir, fileName)
                val outputStream = FileOutputStream(file)
                outputStream.use {stream ->
                    try {
                        stream.write(body.bytes())
                    } catch (e: IOException) {
                        return@withContext Result.failure(
                            workDataOf(
                                REASON to "Unknown",
                                FILENAME to fileName
                            )
                        )
                    }
                    Result.success(
                        workDataOf(
                            FILENAME to file.name,
                        )
                    )
                }
            }
        }

        if(!response.isSuccessful) {
            if(response.code().toString().startsWith("5")) {
                return Result.retry()
            }
            return Result.failure(
                workDataOf(
                    REASON to "Network error",
                    FILENAME to "Unknown_"+System.currentTimeMillis().toString()
                )
            )
        }
        return Result.failure(
            workDataOf(
                REASON to "Unknown error",
                FILENAME to "Unknown_"+System.currentTimeMillis().toString()
            )
        )
    }
}