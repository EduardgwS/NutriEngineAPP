package com.explosionlab.nutriengine

import okhttp3.OkHttpClient


object NetworkModule {
    val httpClient: OkHttpClient by lazy { OkHttpClient() }
}
