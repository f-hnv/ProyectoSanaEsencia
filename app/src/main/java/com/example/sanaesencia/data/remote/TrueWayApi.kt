package com.example.sanaesencia.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class RutaTrueWayRespuesta(val route: RutaTrueWayDto?)
data class RutaTrueWayDto(val distance: Double?, val duration: Double?)

interface TrueWayApi {
    @Headers("X-RapidAPI-Host: trueway-directions2.p.rapidapi.com")
    @GET("FindDrivingRoute")
    suspend fun ruta(@Query("stops") puntos: String,
        @Header("X-RapidAPI-Key") clave: String): Response<RutaTrueWayRespuesta>
}

object ConexionTrueWay {
    // Cliente independiente: nunca envía la autorización de los microservicios.
    fun crear(url: String = "https://trueway-directions2.p.rapidapi.com/"): TrueWayApi =
        Retrofit.Builder().baseUrl(url).client(OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS).retryOnConnectionFailure(false).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(TrueWayApi::class.java)
    val api: TrueWayApi by lazy { crear() }
}
