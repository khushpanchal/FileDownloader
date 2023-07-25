package com.khush.workmanager.bean

import android.net.Uri
import java.util.UUID

data class DownloadItem(val fileName: String, val status: String, val fileUri: Uri? = null, val uuid: UUID? = null, val url: String? = null)
