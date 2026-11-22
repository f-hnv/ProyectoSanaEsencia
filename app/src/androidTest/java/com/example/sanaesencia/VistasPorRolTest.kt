package com.example.sanaesencia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/** Navegación real con login HTTP; el servidor de prueba controla los roles. */
class VistasPorRolTest {
    private val backend=BackendUsuariosFixture()
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val reglas:RuleChain=RuleChain.outerRule(backend).around(compose)
    private fun entrar(rol:String) {
        compose.waitUntil(20_000) { runCatching { compose.onAllNodesWithText("Ingresar").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        compose.onNodeWithText("Correo Electrónico").performTextInput("${rol.lowercase()}.fixture@example.com")
        compose.onNodeWithText("Contraseña").performTextInput(backend.clave)
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("nav-home").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun pacienteConservaInicioYReservas() {
        entrar("PACIENTE")
        compose.onNodeWithText("Tus próximas sesiones").assertExists()
        compose.onNodeWithTag("nav-catalogo_servicios").assertExists()
        compose.onNodeWithText("Mi agenda mensual").assertDoesNotExist()
    }
    @Test fun especialistaVeSuMesYNoPuedeReservar() {
        entrar("ESPECIALISTA")
        compose.onNodeWithTag("agenda-terapeuta").assertExists()
        compose.onNodeWithTag("nav-catalogo_servicios").assertDoesNotExist()
        compose.onNode(hasText("Mi agenda mensual") and hasAnyAncestor(hasTestTag("agenda-terapeuta"))).assertExists()
    }
    @Test fun adminGuardaRolEnServidorYElPacienteCambiaDeVistaAlEntrar() {
        entrar("ADMIN")
        compose.onNodeWithText("Número de cuenta").performTextInput("42")
        compose.onNodeWithText("Buscar cuenta").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("paciente.fixture@example.com").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Especialista").performScrollTo().performClick()
        compose.onNodeWithText("Número del perfil profesional").performScrollTo().performTextInput("1")
        compose.onNodeWithText("Guardar rol").performScrollTo().performClick()
        compose.waitUntil(15_000) { backend.cuentas[42]?.rol=="ESPECIALISTA" }
        assertEquals(1L,backend.cuentas[42]?.especialistaId)
        compose.onNodeWithTag("nav-perfil").performClick()
        compose.onNodeWithText("Cerrar Sesión").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Ingresar").fetchSemanticsNodes().isNotEmpty() }
        entrar("PACIENTE")
        compose.onNodeWithTag("agenda-terapeuta").assertExists()
        compose.onNodeWithTag("nav-catalogo_servicios").assertDoesNotExist()
    }
}
