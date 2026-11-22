package com.example.sanaesencia.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Body
import com.example.sanaesencia.BuildConfig
import java.util.concurrent.TimeUnit

data class RutaSedeRespuesta(val route: RutaSedeDto?)
data class RutaSedeDto(val distance: Double?, val duration: Double?)
data class OrigenRuta(val latitud:Double,val longitud:Double)

interface RutasApi {
    @POST("api/rutas/sede")
    suspend fun ruta(@Body origen:OrigenRuta,
        @Header("Authorization") autorizacion:String):Response<RutaSedeRespuesta>
}

object ConexionRutas {
    // La app habla solo con SanaEsencia. La clave del proveedor permanece en AWS.
    fun crear(url: String = BuildConfig.RESERVAS_API_URL): RutasApi =
        Retrofit.Builder().baseUrl(url).client(OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS).retryOnConnectionFailure(false).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(RutasApi::class.java)
    val api: RutasApi by lazy { crear() }
}
