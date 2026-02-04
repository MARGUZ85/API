package com.example.marsphotos.network

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface SicenetApiService {


    @Headers("Content-Type: text/xml; charset=utf-8")
    @POST("ws/wsalumnos.asmx")
    suspend fun callSoapAction(
        @Header("SOAPAction") action: String,
        @Body body: RequestBody
    ): String
}