package com.example.sanaesencia.data.remote

import com.example.sanaesencia.domain.model.Modalidad
import java.math.BigDecimal
import retrofit2.http.*

data class ReservaSolicitud(val servicioId:Long,val especialistaId:Long,val inicio:String,val modalidad:Modalidad,
    val latitud:Double?=null,val longitud:Double?=null)
data class ReservaRemota(val id:Long,val pacienteId:Long,val pacienteNombre:String,val especialistaId:Long,
    val especialistaNombre:String,val servicioId:Long,val servicioNombre:String,val precio:BigDecimal,
    val inicio:String,val fin:String,val modalidad:Modalidad,val estado:String,val boxId:Long?,val boxNombre:String?,
    val distanciaKm:Double?,val sugiereOnline:Boolean)
data class BoxDisponible(val id:Long,val nombre:String)
data class TurnoRemoto(val inicio:String,val fin:String,val boxesDisponibles:List<BoxDisponible>)
data class SugerenciaRemota(val distanciaKm:Double?,val permitePresencial:Boolean,val permiteOnline:Boolean,
    val sugiereOnline:Boolean,val mensaje:String)
data class HorarioRemoto(val dia:String,val desde:String,val hasta:String,val fecha:String?=null,val cupoPublicado:Boolean=false)
data class HorariosSolicitud(val horarios:List<HorarioRemoto>)
data class BoxRemoto(val id:Long,val nombre:String,val descripcion:String,val activo:Boolean)
data class BoxSolicitud(val nombre:String,val descripcion:String)
data class EstadoBoxSolicitud(val activo:Boolean)
data class EstadoReservaSolicitud(val estado:String)

interface ReservasApi {
    @GET("api/reservas") suspend fun listar(@Query("mes") mes:String,@Header("Authorization") auth:String):List<ReservaRemota>
    @GET("api/reservas/{id}") suspend fun obtener(@Path("id") id:Long,@Header("Authorization") auth:String):ReservaRemota
    @POST("api/reservas") suspend fun crear(@Body solicitud:ReservaSolicitud,@Header("Authorization") auth:String):ReservaRemota
    @PUT("api/reservas/{id}") suspend fun reprogramar(@Path("id") id:Long,@Body solicitud:ReservaSolicitud,@Header("Authorization") auth:String):ReservaRemota
    @DELETE("api/reservas/{id}") suspend fun cancelar(@Path("id") id:Long,@Header("Authorization") auth:String)
    @PATCH("api/reservas/{id}/estado") suspend fun estado(@Path("id") id:Long,@Body estado:EstadoReservaSolicitud,@Header("Authorization") auth:String):ReservaRemota
    @GET("api/disponibilidad") suspend fun disponibilidad(@Query("especialistaId") especialista:Long,@Query("servicioId") servicio:Long,
        @Query("fecha") fecha:String,@Query("modalidad") modalidad:Modalidad,@Query("reservaId") reserva:Long?,@Header("Authorization") auth:String):List<TurnoRemoto>
    @GET("api/ubicacion/sugerencia") suspend fun sugerencia(@Query("especialistaId") especialista:Long,@Query("servicioId") servicio:Long,
        @Query("latitud") latitud:Double?,@Query("longitud") longitud:Double?,@Header("Authorization") auth:String):SugerenciaRemota
    @GET("api/agenda/{id}") suspend fun horarios(@Path("id") especialista:Long,@Header("Authorization") auth:String):List<HorarioRemoto>
    @PUT("api/agenda/{id}") suspend fun guardarHorarios(@Path("id") especialista:Long,@Body horarios:HorariosSolicitud,@Header("Authorization") auth:String):List<HorarioRemoto>
    @GET("api/admin/boxes") suspend fun boxes(@Header("Authorization") auth:String):List<BoxRemoto>
    @POST("api/admin/boxes") suspend fun crearBox(@Body box:BoxSolicitud,@Header("Authorization") auth:String):BoxRemoto
    @PATCH("api/admin/boxes/{id}/estado") suspend fun estadoBox(@Path("id") id:Long,@Body estado:EstadoBoxSolicitud,@Header("Authorization") auth:String):BoxRemoto
}
