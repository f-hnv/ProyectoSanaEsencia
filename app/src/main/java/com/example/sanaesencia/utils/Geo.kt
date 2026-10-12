package com.example.sanaesencia.utils

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// Lógica pura de Kotlin (sin Android): fácil de probar y de explicar en la defensa.
object Geo {

    private const val RADIO_TIERRA_M = 6_371_000.0

    /** Distancia en metros sobre la superficie terrestre entre dos coordenadas (fórmula de Haversine). */
    fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * RADIO_TIERRA_M * asin(sqrt(a))
    }

    /** "850 m" si es menos de 1 km; "12,3 km" en caso contrario. */
    fun formatearDistancia(metros: Double): String =
        if (metros < 1_000) "${metros.roundToInt()} m"
        else String.format(Locale.forLanguageTag("es-CL"), "%.1f km", metros / 1_000)
}

