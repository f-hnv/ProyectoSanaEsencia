package com.example.sanaesencia.data.remote

import okhttp3.Credentials

/** Basic Auth: credenciales solo en memoria; nunca se guardan en preferencias o Room. */
object SesionBackend {
    @Volatile private var sesion: Sesion? = null
    data class Sesion(val usuario: UsuarioRemoto, val autorizacion: String)
    fun actual(): Sesion? = sesion
    fun validar(usuario: UsuarioRemoto) {
        require(usuario.activo && usuario.id > 0 && !usuario.correo.isNullOrBlank() &&
            !usuario.nombre.isNullOrBlank() && !usuario.apellido.isNullOrBlank() && !usuario.run.isNullOrBlank() &&
            usuario.rol in setOf("ADMIN", "PACIENTE", "ESPECIALISTA") &&
            (usuario.rol != "ESPECIALISTA" || (usuario.especialistaId ?: 0) > 0)) {
            "El servidor devolvió una cuenta no válida"
        }
    }
    fun abrir(usuario: UsuarioRemoto, clave: String) {
        validar(usuario)
        sesion = Sesion(usuario, Credentials.basic(usuario.correo, clave, Charsets.UTF_8))
    }
    fun actualizar(usuario: UsuarioRemoto) {
        val anterior = sesion ?: return
        require(anterior.usuario.id == usuario.id && anterior.usuario.correo == usuario.correo)
        sesion = anterior.copy(usuario = usuario)
    }
    fun cerrar() { sesion = null }
}
