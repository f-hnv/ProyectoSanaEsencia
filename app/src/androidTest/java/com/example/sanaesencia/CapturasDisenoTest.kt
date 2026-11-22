package com.example.sanaesencia

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.local.seed.SeedData
import com.example.sanaesencia.domain.model.HorariosAgenda
import com.example.sanaesencia.ui.agenda.AgendaScreen
import com.example.sanaesencia.ui.contacto.ContactoScreen
import com.example.sanaesencia.ui.home.HomeContent
import com.example.sanaesencia.ui.login.LoginScreen
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Generación opcional de evidencia visual con datos ficticios, sin enviar ni confirmar reservas. */
class CapturasDisenoTest {
    @get:Rule val compose = createComposeRule()
    @Before fun soloBajoSolicitud() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("capturas") == "true")
    }
    private fun guardar(nombre: String) {
        compose.waitForIdle()
        val salida = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /sdcard/Download/sanaesencia-$nombre.png")
        ParcelFileDescriptor.AutoCloseInputStream(salida).use { it.readBytes() }
    }
    @Test fun capturaLogin() {
        compose.setContent { SanaesenciaTheme { LoginScreen({}, {}) } }
        guardar("login")
    }
    @Test fun capturaInicio() {
        compose.setContent { SanaesenciaTheme {
            HomeContent("Paciente de prueba", emptyList(), {}, {}, { _, _ -> }, {}, {}, {})
        } }
        guardar("inicio")
    }
    @Test fun capturaContacto() {
        compose.setContent { SanaesenciaTheme { ContactoScreen({}, {}) } }
        guardar("contacto")
    }
    @Test fun capturaAgenda() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SanaesenciaApp
        val servicioId = runBlocking {
            com.example.sanaesencia.data.local.seed.CatalogoCentro.instalar(app.container.database)
            app.container.database.servicioDao().obtenerPorNombre("Psicología infantojuvenil")!!.id
        }
        val vm=com.example.sanaesencia.viewmodel.AgendaViewModel(app.container.citaRepository,
            com.example.sanaesencia.data.repository.ServicioRepository(app.container.database.servicioDao()),
            com.example.sanaesencia.data.repository.EspecialistaRepository(app.container.database.especialistaDao()),"captura@example.com")
        compose.setContent { SanaesenciaTheme { AgendaScreen(servicioIdPreseleccionado=servicioId,viewModelOverride=vm,onNavigateBack={}) } }
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("Samantha Bastías",substring=true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Samantha Bastías",substring=true).performScrollTo().performClick()
        val ahora = System.currentTimeMillis()
        val dia = HorariosAgenda.diasParaElegir(ahora,60,ahora).first()
        // El día vive en una fila horizontal: mostrar primero la zona inferior del formulario.
        compose.onNodeWithText("Confirmar Reserva").performScrollTo()
        compose.onNodeWithTag("dia-$dia").performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
        val horas = HorariosAgenda.bloquesDelDia(dia, 60).map { HorariosAgenda.formatear(it, "HH:mm") }
        compose.waitUntil(15_000) { horas.any { hora ->
            compose.onAllNodes(hasText(hora) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        } }
        val horaLibre = horas.first { compose.onAllNodes(hasText(it) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(horaLibre).performScrollTo().performClick()
        compose.onNodeWithText("Toca una hora disponible").performScrollTo()
        guardar("agenda")
        compose.onNodeWithText("Confirmar Reserva").performScrollTo()
        guardar("resumen")
    }
}
