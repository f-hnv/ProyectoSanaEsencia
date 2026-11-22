package com.example.sanaesencia

import com.example.sanaesencia.utils.ValidacionRegistro
import org.junit.Assert.*
import org.junit.Test

class ValidacionRegistroTest {
    @Test fun runAceptaDigitosYKConGuion() {
        for (run in listOf("12345678-5","1000005-K","1000005-k","10000004-0")) assertNull(run,ValidacionRegistro.run(run))
    }
    @Test fun runRechazaSinGuionPuntosCerosInicialesYDigitoIncorrecto() {
        for (run in listOf("123456785","12.345.678-5","01234567-4","12345678-9","","123-4","123456789-5")) assertNotNull(run,ValidacionRegistro.run(run))
    }
    @Test fun claveRespetaLimitesDeBcryptYFormulario() {
        assertNull(ValidacionRegistro.clave("12345678")); assertNull(ValidacionRegistro.clave("a".repeat(64)))
        for (p in listOf("1234567","a".repeat(65),"界".repeat(25))) assertNotNull(ValidacionRegistro.clave(p))
    }
    @Test fun nombreYApellidoSonObligatoriosYAcotados() {
        assertNull(ValidacionRegistro.nombre("José")); assertNotNull(ValidacionRegistro.nombre("  "))
        assertNotNull(ValidacionRegistro.nombre("a".repeat(101)))
    }
}
