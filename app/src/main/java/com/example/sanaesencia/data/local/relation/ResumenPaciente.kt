package com.example.sanaesencia.data.local.relation

// Una fila por paciente, calculada por SQL (una sola pasada sobre la tabla).
data class ResumenPaciente(
    val emailPaciente: String,
    val completadas: Int,
    val canceladas: Int,
    val inasistencias: Int,
    val futuras: Int,              // citas PROGRAMADAS que aún no ocurren
    val ultimaAtencion: Long?,     // última cita COMPLETADA; null si nunca asistió
    val primeraCita: Long?
)

