package com.example.sanaesencia

import com.example.sanaesencia.domain.model.HorariosAgenda
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class HorariosAgendaTest {
    private fun fecha(dia: Int, hora: Int = 0, minuto: Int = 0): Long =
        Calendar.getInstance(HorariosAgenda.zona).apply {
            clear()
            set(2026, Calendar.OCTOBER, dia, hora, minuto)
        }.timeInMillis

    @Test fun soloHorasFijasDentroDeJornadaSinAlmuerzo() {
        val horas = HorariosAgenda.bloquesDelDia(fecha(19), 50).map { HorariosAgenda.formatear(it, "HH:mm") }
        assertEquals(listOf("09:00", "10:00", "11:00", "12:00", "14:00", "15:00", "16:00", "17:00"), horas)
        assertFalse(HorariosAgenda.esBloqueValido(fecha(19, 10, 23), 50))
    }

    @Test fun noAtiendeFinDeSemana() {
        assertTrue(HorariosAgenda.bloquesDelDia(fecha(17), 50).isEmpty())
        assertTrue(HorariosAgenda.bloquesDelDia(fecha(18), 50).isEmpty())
    }

    @Test fun duracionCompletaDebeCaberSinCruzarAlmuerzoNiCierre() {
        assertFalse(HorariosAgenda.esBloqueValido(fecha(19, 12), 90))
        assertFalse(HorariosAgenda.esBloqueValido(fecha(19, 17), 90))
        assertTrue(HorariosAgenda.esBloqueValido(fecha(19, 11), 120))
        assertTrue(HorariosAgenda.esBloqueValido(fecha(19, 16), 120))
    }

    @Test fun fechasVisiblesExcluyenPasadoFinesDeSemanaYJornadasSinAnticipacion() {
        val dias = HorariosAgenda.diasParaElegir(fecha(16),50,fecha(16,20))
        assertEquals(listOf(19,20,21,22), dias.map { HorariosAgenda.calendario(it).get(Calendar.DAY_OF_MONTH) })
    }
}
