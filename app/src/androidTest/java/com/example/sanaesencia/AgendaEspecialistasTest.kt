package com.example.sanaesencia

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.seed.SeedData
import com.example.sanaesencia.data.repository.CitaRepository
import com.example.sanaesencia.data.repository.ServicioRepository
import com.example.sanaesencia.data.repository.EspecialistaRepository
import com.example.sanaesencia.ui.agenda.AgendaScreen
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.viewmodel.AgendaViewModel
import com.example.sanaesencia.viewmodel.EstadoOperacion
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.example.sanaesencia.domain.model.HorariosAgenda
import com.example.sanaesencia.domain.model.Modalidad
import java.util.Calendar

class AgendaEspecialistasTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun reservaGuardaElEspecialistaElegidoEnRoom() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Base temporal: comprobar la reserva sin alterar los datos de la aplicación.
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val citas = CitaRepository(db)
        val vm = AgendaViewModel(citas, ServicioRepository(db.servicioDao()),
            EspecialistaRepository(db.especialistaDao()), "prueba.agenda@example.com")
        try {
            SeedData.poblarSiVacia(db)
            withTimeout(10_000) { vm.uiState.first { !it.cargandoEspecialistas && it.especialistas.isNotEmpty() } }
            vm.onServicioSeleccionado(1L)
            withTimeout(10_000) { vm.uiState.first { it.servicioSeleccionado != null } }
            vm.onEspecialistaSeleccionado(2L)
            vm.onModalidadSeleccionada(Modalidad.ONLINE)
            vm.onDiaSeleccionado(diaLaboralFuturo())
            val horas = withTimeout(10_000) { vm.uiState.first { it.horariosDisponibles.isNotEmpty() } }
            vm.onFechaHoraSeleccionada(horas.horariosDisponibles.first())
            vm.confirmarAgendamiento()
            val resultado = withTimeout(10_000) {
                vm.uiState.first { it.operacion is EstadoOperacion.Exito || it.operacion is EstadoOperacion.Error }
            }
            assertTrue(resultado.operacion.toString(), resultado.operacion is EstadoOperacion.Exito)
            val guardadas = withTimeout(10_000) {
                citas.observarPorPaciente("prueba.agenda@example.com").first { it.isNotEmpty() }
            }
            assertEquals(2L, guardadas.single().cita.especialistaId)
            assertEquals(Modalidad.ONLINE, guardadas.single().cita.modalidad)
        } finally {
            vm.viewModelScope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
            db.close()
        }
    }

    @Test
    fun reservaExigeElegirEspecialistaYPermiteCambiarLaSeleccion() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SanaesenciaApp
        runBlocking { SeedData.poblarSiVacia(app.container.database) }
        compose.setContent {
            SanaesenciaTheme {
                AgendaScreen(servicioIdPreseleccionado = 1L, onNavigateBack = {})
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("Dra. Paula Demo", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // La reserva debe empezar sin profesional asignado automáticamente.
        compose.onNodeWithText("Dra. Paula Demo", substring = true).assertIsNotSelected()
        compose.onNodeWithText("Ter. Mateo Demo", substring = true).assertIsNotSelected()
        compose.onNodeWithText("Confirmar Reserva").performScrollTo().assertIsNotEnabled()

        compose.onNodeWithText("Ter. Mateo Demo", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Ter. Mateo Demo", substring = true).assertIsSelected()
        compose.onNodeWithText("Confirmar Reserva").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("dia-${HorariosAgenda.inicioDia(diaLaboralFuturo())}")
            .performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("09:00")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("09:00").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar Reserva").performScrollTo().assertIsEnabled()

        compose.onNodeWithText("Dra. Paula Demo", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Dra. Paula Demo", substring = true).assertIsSelected()
        compose.onNodeWithText("Ter. Mateo Demo", substring = true).assertIsNotSelected()
        compose.onNodeWithText("Confirmar Reserva").performScrollTo().assertIsNotEnabled()
    }

    private fun diaLaboralFuturo(): Long {
        val ahora = System.currentTimeMillis()
        return HorariosAgenda.diasParaElegir(ahora, 50, ahora).first()
    }
}
