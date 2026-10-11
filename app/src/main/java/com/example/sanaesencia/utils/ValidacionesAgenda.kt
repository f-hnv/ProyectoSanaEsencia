package com.example.sanaesencia.utils

// Un mensaje por campo. null = el campo está bien. La UI solo pinta el texto si no es null.
data class ErroresFormularioAgenda(
    val servicio: String? = null,
    val especialista: String? = null,
    val fechaHora: String? = null,
    val modalidad: String? = null
) {
    val hayErrores: Boolean
        get() = servicio != null || especialista != null || fechaHora != null || modalidad != null
}

object ValidacionesAgenda {

    fun validar(
        servicioId: Long?,
        especialistaId: Long?,
        fechaHoraInicio: Long?,
        ahora: Long = System.currentTimeMillis()
    ) = ErroresFormularioAgenda(
        servicio = if (servicioId == null) "Selecciona un servicio" else null,
        especialista = if (especialistaId == null) "Selecciona un especialista" else null,
        fechaHora = when {
            fechaHoraInicio == null -> "Selecciona fecha y hora"
            fechaHoraInicio <= ahora -> "La fecha y hora deben ser futuras"
            else -> null
        }
    )
}