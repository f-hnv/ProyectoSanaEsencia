package com.example.sanaesencia

import com.example.sanaesencia.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PeriodoYModalidadTest {
    @Test fun sugerenciaSoloSobreVeinteKmYConServicioCompatible() {
        assertFalse(RecomendacionModalidad.sugerirOnline(null, 20f, true))
        assertFalse(RecomendacionModalidad.sugerirOnline(19_999.0, 20f, true))
        assertFalse(RecomendacionModalidad.sugerirOnline(20_000.0, 20f, true))
        assertTrue(RecomendacionModalidad.sugerirOnline(20_001.0, 20f, true))
        assertFalse(RecomendacionModalidad.sugerirOnline(200_000.0, 20f, false))
    }
    @Test fun sugerenciaAdmiteOtrosPaisesYUmbralConfigurado() {
        assertTrue(RecomendacionModalidad.sugerirOnline(10_000_000.0, 20f, true))
        assertFalse(RecomendacionModalidad.sugerirOnline(25_000.0, 30f, true))
        assertFalse(RecomendacionModalidad.sugerirOnline(Double.NaN, 20f, true))
        assertFalse(RecomendacionModalidad.sugerirOnline(Double.POSITIVE_INFINITY, 20f, true))
    }
    @Test fun mesesCruzanElAnioYRespetanElLimiteExclusivo() {
        val diciembre = PeriodoAgenda(2026, 12)
        assertEquals(PeriodoAgenda(2027, 1), diciembre.desplazar(1))
        assertEquals(PeriodoAgenda(2026, 12), PeriodoAgenda(2027, 1).desplazar(-1))
        assertEquals(diciembre.fin, diciembre.desplazar(1).inicio)
        assertEquals(29, HorariosAgenda.calendario(PeriodoAgenda(2028, 2).fin - 1).get(Calendar.DAY_OF_MONTH))
    }
    @Test fun mesUsaHoraDeChileAunqueElTelefonoEsteEnOtraZona() {
        val anterior = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            val periodo = PeriodoAgenda(2026, 11)
            assertEquals("01/11/2026 00:00", HorariosAgenda.formatear(periodo.inicio, "dd/MM/yyyy HH:mm"))
            assertEquals(PeriodoAgenda(2026, 11), PeriodoAgenda.desde(periodo.fin - 1))
            assertEquals(PeriodoAgenda(2026, 12), PeriodoAgenda.desde(periodo.fin))
        } finally { TimeZone.setDefault(anterior) }
    }
    @Test fun vistasDeClienteProfesionalYAdminNoCompartenLasReservas() {
        assertTrue(VistasCuenta.puedeVerRuta("PACIENTE", "catalogo_servicios"))
        assertTrue(VistasCuenta.puedeVerRuta("PACIENTE", "agenda?servicioId={servicioId}&citaId={citaId}"))
        assertFalse(VistasCuenta.puedeVerRuta("ESPECIALISTA", "agenda"))
        assertFalse(VistasCuenta.puedeVerRuta("ESPECIALISTA", "catalogo_servicios"))
        assertTrue(VistasCuenta.puedeVerRuta("ESPECIALISTA", "home"))
        assertFalse(VistasCuenta.puedeVerRuta("PACIENTE", "administracion"))
        assertFalse(VistasCuenta.puedeVerRuta("ESPECIALISTA", "administracion"))
        assertTrue(VistasCuenta.puedeVerRuta("ADMIN", "administracion"))
        assertFalse(VistasCuenta.puedeVerRuta(null, "home"))
        assertFalse(VistasCuenta.puedeVerRuta("DESCONOCIDO", "home"))
    }
}
