package com.example.sanaesencia

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.domain.model.SedeSanaesencia
import com.example.sanaesencia.nativo.ProveedorUbicacion
import com.example.sanaesencia.nativo.UbicacionResultado
import com.example.sanaesencia.ui.sede.SedeContent
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.viewmodel.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SedeGpsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pantallaExplicaPermisosGpsYDistancia() {
        val estado = mutableStateOf(SedeUiState(EstadoUbicacion.PermisoDenegado))
        var ajustes = 0
        var mapas = 0
        compose.setContent {
            SanaesenciaTheme { SedeContent(estado.value, {}, {}, { ajustes++ }, { mapas++ }) }
        }
        compose.onNodeWithText("No se concedió el permiso", substring=true).assertIsDisplayed()
        compose.onNodeWithText("Abrir ajustes").performScrollTo().performClick()
        assertEquals(1, ajustes)
        compose.runOnIdle { estado.value = SedeUiState(EstadoUbicacion.UbicacionDesactivada) }
        compose.onNodeWithText("La ubicación está desactivada", substring=true).assertIsDisplayed()
        compose.runOnIdle { estado.value = SedeUiState(EstadoUbicacion.Exito(-31.9,-71.5,15f), distanciaTexto="850 m") }
        compose.onNodeWithText("Estás a 850 m de la sede").assertIsDisplayed()
        compose.onNodeWithText("Ver sede en mapas").performScrollTo().performClick()
        assertEquals(1, mapas)
    }

    @Test fun viewModelTraduceErroresYCalculaDistancia() = runBlocking {
        val proveedor = object : ProveedorUbicacion {
            override val permisosRequeridos = emptyArray<String>()
            override fun tienePermiso() = true
            var resultado: UbicacionResultado = UbicacionResultado.SinPermiso
            override suspend fun obtenerUbicacionActual() = resultado
        }
        val vm = SedeViewModel(proveedor)
        try {
            for ((resultado, esperado) in listOf(
                UbicacionResultado.SinPermiso to EstadoUbicacion.PermisoDenegado,
                UbicacionResultado.UbicacionDesactivada to EstadoUbicacion.UbicacionDesactivada,
                UbicacionResultado.NoDisponible to EstadoUbicacion.NoDisponible
            )) {
                proveedor.resultado = resultado
                vm.actualizarUbicacion()
                withTimeout(5_000) { vm.uiState.first { it.estado == esperado } }
            }
            proveedor.resultado = UbicacionResultado.Exito(SedeSanaesencia.LATITUD, SedeSanaesencia.LONGITUD,10f)
            vm.actualizarUbicacion()
            val estado = withTimeout(5_000) { vm.uiState.first { it.estado is EstadoUbicacion.Exito } }
            assertEquals(0.0, estado.distanciaMetros!!, 0.1)
            assertEquals("0 m", estado.distanciaTexto)
        } finally { vm.viewModelScope.cancel() }
    }
}
