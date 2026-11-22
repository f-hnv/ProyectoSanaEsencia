package com.example.sanaesencia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.remote.SesionBackend
import com.example.sanaesencia.data.repository.CuentaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class FlujoMejorasTest {
    private val backend=BackendUsuariosFixture()
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val reglas:RuleChain=RuleChain.outerRule(backend).around(compose)
    private fun entrar() {
        compose.onNodeWithText("Correo Electrónico").performTextInput("paciente.fixture@example.com")
        compose.onNodeWithText("Contraseña").performTextInput(backend.clave)
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("nav-home").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun perfilActualizaServidorYSalirBorraSesion() {
        entrar()
        compose.onNodeWithTag("nav-perfil").performClick()
        compose.onNodeWithText("Nombre").performScrollTo().performTextReplacement("Actualizada")
        compose.onNodeWithText("Apellido").performScrollTo().performTextReplacement("Remota")
        compose.onNodeWithText("Guardar perfil").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Cambios guardados").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("Actualizada",backend.cuentas[42]?.nombre)
        assertEquals("Remota",backend.cuentas[42]?.apellido)
        compose.onNodeWithTag("nav-home").performClick()
        compose.onNodeWithText("Hola, Actualizada Remota").assertExists()
        compose.onNodeWithTag("nav-perfil").performClick()
        compose.onNodeWithText("Cerrar Sesión").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Ingresar").fetchSemanticsNodes().isNotEmpty() }
        assertNull(SesionBackend.actual())
    }
    @Test fun claveIncorrectaNoAbreSesionNiUsaCacheRoom() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking { val repo=CuentaRepository(context); repo.entrar("paciente.fixture@example.com",backend.clave); repo.salir() }
        compose.onNodeWithText("Correo Electrónico").performTextInput("paciente.fixture@example.com")
        compose.onNodeWithText("Contraseña").performTextInput("Incorrecta2026!")
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Correo o contraseña incorrectos, o cuenta desactivada").fetchSemanticsNodes().isNotEmpty() }
        assertNull(SesionBackend.actual())
        compose.onNodeWithTag("nav-home").assertDoesNotExist()
    }
}
