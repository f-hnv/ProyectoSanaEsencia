package com.example.sanaesencia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.local.entity.CitaEntity
import com.example.sanaesencia.data.local.seed.SeedData
import com.example.sanaesencia.data.repository.CuentaRepository
import com.example.sanaesencia.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Recorre el login y la navegación real: los roles no se simulan en el componente de pantalla. */
class VistasPorRolTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun entrar(email: String, clave: String = "Prueba123") {
        compose.onNodeWithText("Correo Electrónico").performTextInput(email)
        compose.onNodeWithText("Contraseña").performTextInput(clave)
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("nav-home").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun pacienteConservaSuInicioYElAccesoALasReservas() {
        val email = "vista.paciente.${System.nanoTime()}@example.com"
        runBlocking { CuentaRepository(context).registrar("Paciente de navegación", email, "Prueba123") }
        entrar(email)
        compose.onNodeWithText("Tus próximas sesiones").assertExists()
        compose.onNodeWithTag("nav-catalogo_servicios").assertExists().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Nuestros Servicios").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Mi agenda mensual").assertDoesNotExist()
        compose.onNodeWithText("Roles de las cuentas").assertDoesNotExist()
    }

    @Test fun especialistaEntraDirectamenteASuMesYVeElBoxSinPoderReservar() {
        val cuenta = CuentaRepository(context)
        val email = "vista.especialista.${System.nanoTime()}@example.com"
        val periodo = PeriodoAgenda.desde()
        var citaId = 0L
        runBlocking {
            SeedData.poblarSiVacia(cuenta.db)
            cuenta.entrar(CuentaRepository.ADMIN_DEMO, CuentaRepository.CLAVE_DEMO)
            cuenta.crearEspecialista("Terapeuta de navegación", email, "Prueba123", "Prueba", "Cuenta ficticia", listOf(1))
            val id = cuenta.db.centroDao().usuario(email)!!.especialistaId!!
            citaId = cuenta.db.citaDao().insertar(CitaEntity(emailPaciente = "paciente.$id@example.com",
                servicioId = 1, especialistaId = id, fechaHoraInicio = periodo.inicio,
                duracionMin = 50, modalidad = Modalidad.PRESENCIAL, boxId = 1))
            cuenta.salir()
        }
        try {
            entrar(email)
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("cita-terapeuta-$citaId").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("agenda-terapeuta").assertExists()
            compose.onNodeWithText("Presencial · Box 1").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("nav-catalogo_servicios").assertDoesNotExist()
            compose.onNodeWithText("Tus próximas sesiones").assertDoesNotExist()
            compose.onNodeWithTag("mes-siguiente").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText(periodo.desplazar(1).titulo).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("cita-terapeuta-$citaId").assertDoesNotExist()
            compose.onNodeWithTag("mes-anterior").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("cita-terapeuta-$citaId").fetchSemanticsNodes().isNotEmpty() }
        } finally {
            // Solo se retira la cita ficticia creada por esta prueba, nunca los datos del usuario.
            runBlocking {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    cuenta.db.openHelper.writableDatabase.execSQL("DELETE FROM citas WHERE id = ?", arrayOf(citaId))
                }
                cuenta.salir()
            }
        }
    }

    @Test fun adminAsignaEspecialistaDesdeElPanelYElLoginCambiaDeVista() {
        val cuenta = CuentaRepository(context)
        val sufijo = System.nanoTime()
        val paciente = "panel.paciente.$sufijo@example.com"
        val perfilCuenta = "panel.perfil.$sufijo@example.com"
        val profesional = "Profesional Panel $sufijo"
        runBlocking {
            SeedData.poblarSiVacia(cuenta.db)
            cuenta.registrar("Paciente del panel", paciente, "Prueba123")
            cuenta.entrar(CuentaRepository.ADMIN_DEMO, CuentaRepository.CLAVE_DEMO)
            cuenta.crearEspecialista(profesional, perfilCuenta, "Prueba123", "Prueba", "Datos ficticios", listOf(1))
            cuenta.cambiarRol(perfilCuenta, "PACIENTE", null)
            cuenta.salir()
        }
        entrar(CuentaRepository.ADMIN_DEMO, CuentaRepository.CLAVE_DEMO)
        compose.onNodeWithText("Buscar cuenta por nombre o correo").performScrollTo().performTextInput(paciente)
        compose.onNodeWithText("Especialista").performScrollTo().performClick()
        compose.onNodeWithText("$profesional · Prueba").performScrollTo().performClick()
        compose.onNodeWithTag("guardar-rol-$paciente").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { cuenta.db.centroDao().usuario(paciente)?.rol == "ESPECIALISTA" } }
        compose.onNodeWithContentDescription("Abrir menú").performClick()
        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Correo Electrónico").fetchSemanticsNodes().isNotEmpty() }
        entrar(paciente)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Mi agenda mensual").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("nav-catalogo_servicios").assertDoesNotExist()
    }
}
