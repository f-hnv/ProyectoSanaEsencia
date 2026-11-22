package com.example.sanaesencia.data.remote

import java.math.BigDecimal
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class ServicioRemoto(val id: Long, val nombre: String, val descripcion: String,
    val precio: BigDecimal, val duracionMinutos: Int, val permiteOnline: Boolean, val activo: Boolean)
data class EspecialistaRemoto(val id: Long, val nombre: String, val especialidad: String,
    val descripcion: String, val permitePresencial: Boolean, val permiteOnline: Boolean,
    val activo: Boolean, val servicios: List<ServicioRemoto>)

interface CatalogoApi {
    @GET("api/servicios") suspend fun servicios(): List<ServicioRemoto>
    @GET("api/servicios/{id}") suspend fun servicio(@Path("id") id: Long): ServicioRemoto
    @GET("api/especialistas") suspend fun especialistas(@Query("servicioId") servicioId: Long? = null): List<EspecialistaRemoto>
    @GET("api/especialistas/{id}") suspend fun especialista(@Path("id") id: Long): EspecialistaRemoto
}
