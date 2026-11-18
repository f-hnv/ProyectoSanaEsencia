package com.example.sanaesencia
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.repository.CuentaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class FlujoMejorasTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun captura(nombre:String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(300,3000)
        val descriptor=InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /sdcard/Download/sanaesencia-octubre-$nombre.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use{it.readBytes()}
    }
    @Test fun barraNavegaPerfilYMensajeriaYConservaTelefono() {
        val email="flujo.${System.nanoTime()}@gmail.com"
        runBlocking { CuentaRepository(InstrumentationRegistry.getInstrumentation().targetContext).registrar("Paciente Flujo",email,"Prueba123") }
        compose.onNodeWithText("Correo Electrónico").performTextInput(email)
        compose.onNodeWithText("Contraseña").performTextInput("Prueba123")
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.waitUntil(15_000){compose.onAllNodesWithText("Tus próximas sesiones").fetchSemanticsNodes().isNotEmpty()}
        captura("inicio")
        compose.onNodeWithTag("nav-perfil").performClick()
        compose.waitUntil(10_000){compose.onAllNodesWithText(email).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Teléfono de contacto").performScrollTo().performTextInput("+56 9 1234 5678")
        compose.onNodeWithText("Guardar perfil").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick){it()}
        captura("perfil-antes")
        try { compose.waitUntil(10_000){compose.onAllNodesWithText("Cambios guardados").fetchSemanticsNodes().isNotEmpty()} } catch(e:Exception) { compose.onRoot().printToLog("PERFIL_PRUEBA");throw e }
        captura("perfil")
        compose.onNodeWithTag("nav-mensajes").performClick()
        compose.waitUntil(10_000){compose.onAllNodesWithText("Escribe un mensaje").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Escribe un mensaje").performTextInput("Quisiera consultar por una sesión")
        compose.onNodeWithText("Guardar mensaje local").performClick()
        compose.waitUntil(10_000){compose.onAllNodesWithText("Quisiera consultar por una sesión").fetchSemanticsNodes().isNotEmpty()}
        captura("mensajes")
        compose.onNodeWithTag("nav-perfil").performClick()
        compose.waitUntil(10_000){compose.onAllNodesWithText("+56 9 1234 5678").fetchSemanticsNodes().isNotEmpty()}
    }
    @Test fun administracionAbreDesdeElMenuSoloConSuCuenta() {
        runBlocking { CuentaRepository(InstrumentationRegistry.getInstrumentation().targetContext).prepararDemostracion() }
        captura("login")
        compose.onNodeWithText("Correo Electrónico").performTextInput(CuentaRepository.ADMIN_DEMO)
        compose.onNodeWithText("Contraseña").performTextInput(CuentaRepository.CLAVE_DEMO)
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.waitUntil(15_000){compose.onAllNodesWithText("Tus próximas sesiones").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("Abrir menú").performClick()
        compose.onNodeWithText("Administración").performClick()
        compose.waitUntil(10_000){compose.onAllNodesWithText("Crear profesional y cuenta").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Nombre del profesional").assertIsDisplayed()
        captura("administracion")
    }

}
