package com.khush.workmanager.util

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

interface DownloadInterface {

    @GET
    suspend fun downloadFile(@Url url: String): Response<ResponseBody>

    companion object {
        val instance by lazy {
            Retrofit.Builder().baseUrl("http://localhost")
                .build()
                .create(DownloadInterface::class.java)
        }
    }

}