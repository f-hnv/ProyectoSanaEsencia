package com.example.sanaesencia

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.remote.TrueWayApi
import com.example.sanaesencia.data.remote.RutaTrueWayDto
import com.example.sanaesencia.data.remote.RutaTrueWayRespuesta
import com.example.sanaesencia.data.repository.*
import com.example.sanaesencia.ui.sede.RecorridoContent
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RecorridoSedeTest {
    @get:Rule val compose = createComposeRule()
    @Test fun noConsultaHastaPulsarYExplicaElUsoDeUbicacion() {
        var consultas = 0
        compose.setContent { SanaesenciaTheme { RecorridoContent(RutaUiState.Inicial) { consultas++ } } }
        assertEquals(0,consultas)
        compose.onNodeWithText("Al consultar, compartes tu ubicación",substring=true).assertIsDisplayed()
        compose.onNodeWithText("Consultar recorrido con TrueWay").performClick()
        assertEquals(1,consultas)
    }
    @Test fun cargaDesactivaBotonYMuestraResultadoOReintento() {
        val estado = mutableStateOf<RutaUiState>(RutaUiState.Cargando)
        var consultas = 0
        compose.setContent { SanaesenciaTheme { RecorridoContent(estado.value) { consultas++ } } }
        compose.onNodeWithText("Consultar recorrido con TrueWay").assertIsNotEnabled()
        compose.runOnIdle { estado.value = RutaUiState.Exito(RecorridoSede(1825.0,369.0)) }
        compose.onNodeWithText("1,8 km · 7 min").assertIsDisplayed()
        compose.runOnIdle { estado.value = RutaUiState.Error("Sin conexión") }
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithText("Reintentar recorrido").performClick()
        assertEquals(1,consultas)
    }
    @Test fun viewModelEvitaDobleConsultaYCancelaLaUbicacionAnterior() = runBlocking {
        var consultas = 0
        val pendiente = CompletableDeferred<RutaTrueWayRespuesta?>()
        val api = object: TrueWayApi {
            override suspend fun ruta(puntos:String,clave:String): retrofit2.Response<RutaTrueWayRespuesta> {
                consultas++
                return retrofit2.Response.success(pendiente.await())
            }
        }
        val vm = RutasViewModel(RutasRepository(api) { "prueba" })
        try {
            compose.runOnIdle { vm.consultar(-31.9,-71.5); vm.consultar(-31.9,-71.5) }
            compose.waitUntil(5000) { consultas == 1 }
            assertTrue(vm.estado.value is RutaUiState.Cargando)
            compose.runOnIdle { vm.limpiar() }
            pendiente.complete(RutaTrueWayRespuesta(RutaTrueWayDto(1825.0,369.0)))
            compose.waitForIdle()
            assertEquals(RutaUiState.Inicial,vm.estado.value)
        } finally { vm.viewModelScope.cancel() }
    }
    @Test fun llamadaRealDesdeAndroidCuandoSeConfiguroLaClave() = runBlocking {
        org.junit.Assume.assumeTrue(BuildConfig.TRUEWAY_API_KEY.isNotBlank())
        val repo = RutasRepository(com.example.sanaesencia.data.remote.ConexionTrueWay.api) { BuildConfig.TRUEWAY_API_KEY }
        val resultado = repo.consultar(-31.9,-71.5)
        assertTrue(resultado.metros > 0); assertTrue(resultado.segundos > 0)
    }
}
