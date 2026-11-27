package com.callai.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.Body

// Android emulator -> host computer localhost. For a real device, use your PC's LAN IP.
private const val BASE_URL = "http://10.0.2.2:8000/"

interface CallAiApi {
    @GET("api/v1/dashboard/overview") suspend fun overview(): DashboardOverview
    @GET("api/v1/calls") suspend fun calls(): List<CallItem>
    @GET("api/v1/calls/{id}") suspend fun detail(@Path("id") id: String): CallDetail
    @GET("api/v1/settings/ai") suspend fun settings(): AiSettings
    @PUT("api/v1/settings/ai") suspend fun updateSettings(@Body settings: AiSettings): AiSettings
    @POST("api/v1/demo/calls") suspend fun createDemoCall(): DemoResponse
}

object ApiClient {
    val api: CallAiApi by lazy {
        Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(CallAiApi::class.java)
    }
}
