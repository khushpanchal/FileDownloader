package com.khush.workmanager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.khush.workmanager.MainActivity
import com.khush.workmanager.R
import com.khush.workmanager.util.Const.FILENAME
import com.khush.workmanager.util.Const.KEY_URL
import com.khush.workmanager.util.Const.NOTIFICATION_ID
import com.khush.workmanager.util.Const.REASON
import com.khush.workmanager.util.Const.URL
import com.khush.workmanager.util.DownloadInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    @RequiresApi(Build.VERSION_CODES.M)
    override suspend fun doWork(): Result {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sendNotification()
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
                        cancelNotification()
                        return@withContext Result.failure(
                            workDataOf(
                                REASON to "Unknown",
                                FILENAME to fileName
                            )
                        )
                    }
                    cancelNotification()
                    Result.success(
                        workDataOf(
                            FILENAME to file.name,
                        )
                    )
                }
            }
        }

        cancelNotification()
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

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun sendNotification() {
        val channel = NotificationChannel("download_channel", "File Download", NotificationManager.IMPORTANCE_HIGH)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("notification_id", NOTIFICATION_ID)
        val pendingIntent = getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        setForeground(
            ForegroundInfo(
                NOTIFICATION_ID,
                NotificationCompat.Builder(context, "download_channel")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentText("Downloading...")
                    .setContentTitle("Download in progress")
                    .setContentIntent(pendingIntent)
                    .build()
            )
        )
    }

    private suspend fun cancelNotification() {
        val notificationManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(NotificationManager::class.java)
        } else {
            null
        }
        notificationManager?.cancel(NOTIFICATION_ID)

    }
}