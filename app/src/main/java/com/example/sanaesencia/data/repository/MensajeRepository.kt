package com.example.sanaesencia.data.repository
import android.content.Context
import com.example.sanaesencia.data.local.entity.MensajeEntity

/** Bandeja académica entre cuentas del mismo dispositivo, sin transporte remoto. */
class MensajeRepository(context: Context) {
    private val cuenta = CuentaRepository(context)
    private val dao = cuenta.db.centroDao()
    suspend fun enviar(paciente: String, texto: String) {
        val u = cuenta.actual() ?: error("Inicia sesión")
        require(u.email == paciente || u.rol != "PACIENTE") { "No puedes escribir en otra conversación" }
        require(dao.usuario(paciente)?.rol == "PACIENTE") { "Selecciona un paciente registrado" }
        require(texto.trim().length in 1..2000) { "El mensaje debe tener entre 1 y 2000 caracteres" }
        dao.insertarMensaje(MensajeEntity(pacienteEmail=paciente,remitenteEmail=u.email,
            remitenteNombre=u.nombre,texto=texto.trim()))
    }
    fun observar(paciente: String): kotlinx.coroutines.flow.Flow<List<MensajeEntity>> = kotlinx.coroutines.flow.flow {
        val u=cuenta.actual() ?: error("Inicia sesión")
        require(u.email==paciente || u.rol!="PACIENTE"){"No puedes leer otra conversación"}
        dao.mensajes(paciente).collect { emit(it) }
    }
}
