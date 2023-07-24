package com.khush.workmanager.bean

import android.net.Uri

data class DownloadItem(val fileName: String, val status: String, val fileUri: Uri? = null, val uuid: String? = null)
