package com.example.sanaesencia

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.nativo.LocationHelper
import com.example.sanaesencia.nativo.UbicacionResultado
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.material3.Text

/** En el emulador se inyecta una coordenada de prueba antes de ejecutar esta clase. */
class GpsLecturaTest {
    @get:Rule val compose = createComposeRule()
    @Test fun obtieneLecturaDelProveedorGpsDelEmulador() = runBlocking {
        // El permiso es de uso en primer plano: mantener una Activity visible durante la lectura.
        compose.setContent { Text("Comprobando GPS") }
        compose.waitForIdle()
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        val context = instrumentacion.targetContext
        instrumentacion.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        instrumentacion.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        val resultado = LocationHelper(context).obtenerUbicacionActual()
        assertTrue("No llegó una lectura GPS: $resultado", resultado is UbicacionResultado.Exito)
        resultado as UbicacionResultado.Exito
        assertTrue(resultado.latitud in -90.0..90.0)
        assertTrue(resultado.longitud in -180.0..180.0)
    }
}
