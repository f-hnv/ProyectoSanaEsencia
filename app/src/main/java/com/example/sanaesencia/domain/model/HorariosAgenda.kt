package com.example.sanaesencia.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Horario del centro, expresado en la zona de Chile, independiente de la zona del teléfono. */
object HorariosAgenda {
    val zona: TimeZone get() = TimeZone.getTimeZone("America/Santiago")
    const val ANTICIPACION_MS = 12 * 60 * 60 * 1000L
    const val DESCRIPCION = "Lunes a viernes · 09:00–18:00 · Almuerzo 13:00–14:00 · Hora de Chile"

    fun calendario(instante: Long) = Calendar.getInstance(zona).apply { timeInMillis = instante }

    fun inicioDia(instante: Long): Long = calendario(instante).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun siguienteDia(instante: Long): Long = calendario(inicioDia(instante)).apply {
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    fun bloquesDelDia(dia: Long, duracionMin: Int): List<Long> {
        if (duracionMin <= 0) return emptyList()
        val calendario = calendario(inicioDia(dia))
        if (calendario.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) return emptyList()
        return (9 until 18).mapNotNull { hora ->
            val inicioMin = hora * 60
            val finMin = inicioMin + duracionMin
            if (finMin > 18 * 60 || (inicioMin < 14 * 60 && finMin > 13 * 60)) null
            else (calendario.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, hora) }.timeInMillis
        }
    }

    fun esBloqueValido(inicio: Long, duracionMin: Int): Boolean =
        inicio in bloquesDelDia(inicio, duracionMin)

    /** Días de una semana que aún pueden ofrecer bloques con la anticipación requerida. */
    fun diasParaElegir(inicioSemana: Long, duracionMin: Int, ahora: Long): List<Long> =
        (0..6).map { desplazamiento ->
            calendario(inicioDia(inicioSemana)).apply { add(Calendar.DAY_OF_YEAR, desplazamiento) }.timeInMillis
        }.filter { dia -> bloquesDelDia(dia, duracionMin).any { it - ahora >= ANTICIPACION_MS } }

    fun formatear(instante: Long, patron: String): String =
        SimpleDateFormat(patron, Locale.forLanguageTag("es-CL")).apply { timeZone = zona }.format(instante)
}
