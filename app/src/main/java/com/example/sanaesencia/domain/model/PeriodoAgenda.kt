package com.example.sanaesencia.domain.model

import java.util.Calendar

/** Mes de la agenda del centro. El límite final es exclusivo, también en cambios de año y de horario. */
data class PeriodoAgenda(val anio: Int, val mes: Int) {
    init { require(mes in 1..12) }
    val inicio: Long get() = Calendar.getInstance(HorariosAgenda.zona).apply {
        clear(); set(anio, mes - 1, 1, 0, 0, 0)
    }.timeInMillis
    val fin: Long get() = HorariosAgenda.calendario(inicio).apply { add(Calendar.MONTH, 1) }.timeInMillis
    val titulo: String get() = HorariosAgenda.formatear(inicio, "MMMM yyyy")
    fun desplazar(meses: Int): PeriodoAgenda = desde(HorariosAgenda.calendario(inicio).apply {
        add(Calendar.MONTH, meses)
    }.timeInMillis)
    companion object {
        fun desde(instante: Long = System.currentTimeMillis()): PeriodoAgenda {
            val c = HorariosAgenda.calendario(instante)
            return PeriodoAgenda(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
        }
    }
}
