package com.example.marsphotos.data

import com.example.marsphotos.network.SicenetApiService
import com.example.marsphotos.network.SicenetInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

interface AppContainer {
    val sicenetRepository: SicenetRepository
}

class DefaultAppContainer : AppContainer {
    private val baseUrl = "https://sicenet.surguanajuato.tecnm.mx/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(SicenetInterceptor())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()

    private val retrofitService: SicenetApiService by lazy {
        retrofit.create(SicenetApiService::class.java)
    }

    override val sicenetRepository: SicenetRepository by lazy {
        SicenetRepository()
    }
}