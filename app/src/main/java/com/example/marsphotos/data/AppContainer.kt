package com.example.marsphotos.data

import android.content.Context
import com.example.marsphotos.data.local.SicenetDatabase
import com.example.marsphotos.network.SicenetApiService
import com.example.marsphotos.network.SicenetInterceptor
import com.example.marsphotos.network.SicenetService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

interface AppContainer {
    val sicenetRepository: SicenetRepository
    val sicenetLocalRepository: SicenetLocalRepository
    val sicenetService: SicenetService
}

class DefaultAppContainer(private val context: Context) : AppContainer {

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

    override val sicenetService: SicenetService by lazy {
        SicenetService()
    }

    override val sicenetRepository: SicenetRepository by lazy {
        SicenetRepository(sicenetService)
    }

    private val sicenetDatabase: SicenetDatabase by lazy {
        SicenetDatabase.getDatabase(context)
    }

    override val sicenetLocalRepository: SicenetLocalRepository by lazy {
        SicenetLocalRepository(sicenetDatabase.sicenetDao())
    }
}