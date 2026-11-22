package com.example.sanaesencia.data.remote

import retrofit2.http.*

data class RegistroRequest(val nombre: String, val apellido: String, val run: String, val correo: String, val password: String)
data class LoginRequest(val correo: String, val password: String)
data class PerfilRequest(val nombre: String, val apellido: String)
data class RolRequest(val rol: String, val especialistaId: Long?)
data class UsuarioRemoto(val id: Long, val nombre: String, val apellido: String, val run: String,
    val correo: String, val rol: String, val especialistaId: Long?, val activo: Boolean)

interface UsuariosApi {
    @POST("api/usuarios/registro") suspend fun registrar(@Body solicitud: RegistroRequest): UsuarioRemoto
    @POST("api/usuarios/login") suspend fun login(@Body solicitud: LoginRequest): UsuarioRemoto
    @GET("api/usuarios/{id}") suspend fun obtener(@Path("id") id: Long, @Header("Authorization") autorizacion: String): UsuarioRemoto
    @PUT("api/usuarios/{id}") suspend fun editar(@Path("id") id: Long, @Header("Authorization") autorizacion: String,
        @Body solicitud: PerfilRequest): UsuarioRemoto
    @PATCH("api/admin/usuarios/{id}/rol") suspend fun cambiarRol(@Path("id") id: Long,
        @Header("Authorization") autorizacion: String, @Body solicitud: RolRequest): UsuarioRemoto
}

data class PerfilProfesionalRemoto(val id: Long, val nombre: String, val activo: Boolean)
interface PerfilesApi {
    @GET("api/especialistas/{id}") suspend fun obtener(@Path("id") id: Long,
        @Header("Authorization") autorizacion: String): PerfilProfesionalRemoto
}
