package com.example.bidcarftagent.presentation.di

import com.example.bidcarftagent.data.network.ApiService
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import android.util.Log
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.bidcarftagent.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // BASE_URL is provided via BuildConfig so it can be overridden per-flavor or via gradle.properties
    private val BASE_URL: String = com.example.bidcarftagent.BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val headerInterceptor = Interceptor { chain ->
            val req = chain.request()
            val authHeader = req.header("Authorization") ?: ""
            val shortAuth = if (authHeader.startsWith("Bearer ")) "Bearer <token len=${authHeader.length - 7}>" else authHeader
            Log.d("ApiInterceptor", "Request: ${req.method} ${req.url} auth=$shortAuth")
            val resp = chain.proceed(req)
            Log.d("ApiInterceptor", "Response: ${resp.code} for ${req.url}")
            resp
        }
        return OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(logging)
            // Increase timeouts for long-running LLM requests
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .readTimeout(java.time.Duration.ofSeconds(120))
            .writeTimeout(java.time.Duration.ofSeconds(120))
            .callTimeout(java.time.Duration.ofSeconds(180))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(@ApplicationContext ctx: Context, client: OkHttpClient): Retrofit {
        val baseUrl = try { ctx.getString(R.string.server_base_url) } catch (_: Exception) { BASE_URL }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}

