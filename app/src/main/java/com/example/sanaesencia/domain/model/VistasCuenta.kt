package com.example.sanaesencia.domain.model

/** Las cuentas se registran como pacientes; solo administración puede asignar el rol especialista. */
object VistasCuenta {
    fun puedeReservar(rol: String?) = rol == "PACIENTE"
    fun puedeAdministrar(rol: String?) = rol == "ADMIN"
    fun puedeVerRuta(rol: String?, ruta: String): Boolean = when {
        ruta == "home" || ruta == "perfil" || ruta == "mensajes" -> rol in setOf("PACIENTE", "ESPECIALISTA", "ADMIN")
        ruta == "administracion" -> puedeAdministrar(rol)
        ruta == "catalogo_servicios" || ruta.startsWith("agenda") -> puedeReservar(rol)
        ruta == "sede" || ruta == "contacto" -> rol in setOf("PACIENTE", "ESPECIALISTA", "ADMIN")
        else -> false
    }
}

object RecomendacionModalidad {
    fun sugerirOnline(distanciaMetros: Double?, umbralKm: Float, permiteOnline: Boolean): Boolean =
        permiteOnline && distanciaMetros != null && distanciaMetros.isFinite() &&
            umbralKm.isFinite() && umbralKm > 0 && distanciaMetros > umbralKm * 1000.0
}
