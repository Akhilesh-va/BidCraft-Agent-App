package com.example.bidcarftagent.data.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/auth/verify-token")
    suspend fun verifyToken(@Body body: Map<String, String>): Response<ResponseBody>

    @Multipart
    @POST("api/provider/profile/upload")
    suspend fun uploadProfile(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @PUT("api/provider/profile")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body profile: Map<String, Any>
    ): Response<ResponseBody>

    @GET("api/provider/profile")
    suspend fun getProfile(@Header("Authorization") authorization: String): Response<ResponseBody>

    @Multipart
    @POST("api/parse/srs/upload/overview")
    suspend fun uploadSrs(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @POST("api/bidcraft/generate-proposal")
    suspend fun generateProposal(
        @Header("Authorization") authorization: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>
}

