package com.example.sanaesencia.domain.model

enum class NivelRiesgo(val etiqueta: String) {
    BAJO("Bajo"), MEDIO("Medio"), ALTO("Alto")
}

data class AlertaDesercion(
    val emailPaciente: String,
    val puntaje: Int,                 // 0..100
    val nivel: NivelRiesgo,
    val motivos: List<String>,        // explicación legible en español de por qué tiene ese riesgo
    val completadas: Int,
    val canceladas: Int,
    val inasistencias: Int,
    val citasFuturas: Int,
    val diasSinAtencion: Int
)