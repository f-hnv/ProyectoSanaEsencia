package com.example.sanaesencia.domain.analytics

import com.example.sanaesencia.data.local.relation.ResumenPaciente
import com.example.sanaesencia.domain.model.AlertaDesercion
import com.example.sanaesencia.domain.model.NivelRiesgo
import kotlin.math.roundToInt

object CalculadorRiesgo {

    private const val MS_POR_DIA = 86_400_000L
    private const val PUNTOS_POR_INASISTENCIA = 20
    private const val MAX_PUNTOS_INASISTENCIAS = 40
    private const val MAX_PUNTOS_CANCELACIONES = 30
    private const val MIN_CITAS_PARA_TASA = 2     // evita penalizar una única cancelación
    private const val UMBRAL_ALTO = 60
    private const val UMBRAL_MEDIO = 30

    /** Devuelve null si el paciente aún no tiene citas pasadas (no hay historial que evaluar). */
    fun evaluar(resumen: ResumenPaciente, ahora: Long): AlertaDesercion? {
        val citasResueltas = resumen.completadas + resumen.canceladas + resumen.inasistencias
        if (citasResueltas == 0) return null

        val referencia = resumen.ultimaAtencion ?: resumen.primeraCita ?: ahora
        val diasSinAtencion = ((ahora - referencia) / MS_POR_DIA).toInt().coerceAtLeast(0)
        val tasaCancelacion = resumen.canceladas.toDouble() / citasResueltas

        val ptsInasistencias =
            (resumen.inasistencias * PUNTOS_POR_INASISTENCIA).coerceAtMost(MAX_PUNTOS_INASISTENCIAS)

        val ptsCancelaciones =
            if (citasResueltas >= MIN_CITAS_PARA_TASA) (tasaCancelacion * MAX_PUNTOS_CANCELACIONES).roundToInt()
            else 0

        // Si ya tiene una cita futura agendada sigue comprometido: la inactividad no suma.
        val ptsInactividad = if (resumen.futuras > 0) 0 else when {
            diasSinAtencion >= 60 -> 30
            diasSinAtencion >= 45 -> 20
            diasSinAtencion >= 30 -> 10
            else -> 0
        }

        val puntaje = (ptsInasistencias + ptsCancelaciones + ptsInactividad).coerceIn(0, 100)

        val motivos = buildList {
            if (ptsInasistencias > 0) {
                add("${resumen.inasistencias} inasistencia(s)")
            }
            if (ptsCancelaciones > 0) {
                add("Cancela el ${(tasaCancelacion * 100).roundToInt()}% de sus citas")
            }
            if (ptsInactividad > 0) {
                add("Sin atención hace $diasSinAtencion días y sin cita agendada")
            }
        }

        return AlertaDesercion(
            emailPaciente = resumen.emailPaciente,
            puntaje = puntaje,
            nivel = when {
                puntaje >= UMBRAL_ALTO -> NivelRiesgo.ALTO
                puntaje >= UMBRAL_MEDIO -> NivelRiesgo.MEDIO
                else -> NivelRiesgo.BAJO
            },
            motivos = motivos,
            completadas = resumen.completadas,
            canceladas = resumen.canceladas,
            inasistencias = resumen.inasistencias,
            citasFuturas = resumen.futuras,
            diasSinAtencion = diasSinAtencion
        )
    }
}

