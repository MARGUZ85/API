package com.example.marsphotos.network

import okhttp3.Interceptor
import okhttp3.Response

class SicenetInterceptor : Interceptor {
    companion object {
        var currentCookie: String? = null
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        currentCookie?.let {
            requestBuilder.addHeader("Cookie", it)
        }

        val response = chain.proceed(requestBuilder.build())

        val cookies = response.headers("Set-Cookie")
        if (cookies.isNotEmpty()) {
            currentCookie = cookies[0].split(";")[0]
        }

        return response
    }
}