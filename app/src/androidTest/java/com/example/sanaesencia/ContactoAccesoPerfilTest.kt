package com.example.sanaesencia

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.domain.model.ContactoSanaesencia
import com.example.sanaesencia.ui.contacto.ContactoScreen
import com.example.sanaesencia.ui.login.LoginScreen
import com.example.sanaesencia.viewmodel.LoginViewModel
import com.example.sanaesencia.ui.perfil.PerfilScreen
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import com.example.sanaesencia.data.remote.SesionBackend

class ContactoAccesoPerfilTest {
    private val backend=BackendUsuariosFixture()
    private val compose = createComposeRule()
    @get:Rule val reglas:RuleChain=RuleChain.outerRule(backend).around(compose)
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun contactoUsaNumeroOficialYAbreAlternativaSiNoHayWhatsapp() {
        val enviados = mutableListOf<Intent>()
        val destino = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                enviados += intent
                if (intent.`package` == "com.whatsapp") throw ActivityNotFoundException()
            }
        }
        compose.setContent { CompositionLocalProvider(LocalContext provides destino) {
            SanaesenciaTheme { ContactoScreen({}, {}) }
        } }
        compose.onNodeWithText("Escribir por WhatsApp").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(2, enviados.size)
            val uri = enviados.last().data!!
            assertEquals(Intent.ACTION_VIEW, enviados.last().action)
            assertEquals("wa.me", uri.host)
            assertEquals("/${ContactoSanaesencia.WHATSAPP_NUMERO}", uri.path)
            assertEquals(ContactoSanaesencia.MENSAJE, uri.getQueryParameter("text"))
            assertNull(enviados.last().`package`)
        }
    }

    @Test fun contactoMuestraAyudaSiNoPuedeAbrirNingunaAplicacion() {
        val destino = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { throw ActivityNotFoundException() }
        }
        compose.setContent { CompositionLocalProvider(LocalContext provides destino) {
            SanaesenciaTheme { ContactoScreen({}, {}) }
        } }
        compose.onNodeWithText("Escribir por WhatsApp").performScrollTo().performClick()
        compose.onNodeWithText("No se pudo abrir WhatsApp", substring=true).performScrollTo().assertIsDisplayed()
    }

    @Test fun accesoValidoMuestraBienvenidaYNavegaUnaSolaVez() {
        val vm = LoginViewModel()
        var navegaciones = 0
        try {
            compose.setContent { CompositionLocalProvider(LocalContext provides context) {
                SanaesenciaTheme { LoginScreen({ navegaciones++ }, {}, viewModel=vm) }
            } }
            compose.onNodeWithText("Correo Electrónico").performTextInput("paciente.fixture@example.com")
            compose.onNodeWithText("Contraseña").performTextInput(backend.clave)
            compose.onNodeWithText("Ingresar").performScrollTo().performClick()
            compose.waitUntil(15_000) { vm.accesoConcedido.value }
            compose.onNodeWithText("¡Bienvenido a Sanaesencia!").performScrollTo().assertIsDisplayed()
            compose.waitUntil(15_000) { navegaciones == 1 }
            compose.runOnIdle { assertEquals(1,navegaciones) }
            assertEquals("paciente.fixture@example.com",SesionBackend.actual()?.usuario?.correo)
        } finally {
            runBlocking { vm.viewModelScope.coroutineContext[Job]?.cancelAndJoin() }
        }
    }

    @Test fun accesoInvalidoNoMuestraBienvenidaNiNavega() {
        var navegaciones = 0
        compose.setContent { SanaesenciaTheme { LoginScreen({ navegaciones++ }, {}) } }
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.onNodeWithText("El correo es obligatorio").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("¡Bienvenido a Sanaesencia!").assertDoesNotExist()
        assertEquals(0,navegaciones)
    }

    @Test fun permisoCamaraDenegadoMantienePerfilYOfreceGaleria() {
        val destino = object : ContextWrapper(context) {
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int =
                if (permission == Manifest.permission.CAMERA) PackageManager.PERMISSION_DENIED
                else super.checkPermission(permission,pid,uid)
        }
        val registro = object : ActivityResultRegistry() {
            override fun <I,O> onLaunch(requestCode: Int, contract: ActivityResultContract<I,O>, input: I,
                options: ActivityOptionsCompat?) { dispatchResult(requestCode,false) }
        }
        val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registro }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides destino, LocalActivityResultRegistryOwner provides owner) {
                SanaesenciaTheme { PerfilScreen({}, {}) }
            }
        }
        compose.onNodeWithText("Cámara").performScrollTo().performClick()
        compose.onNodeWithText("No se concedió el permiso de cámara",substring=true).assertIsDisplayed()
        compose.onNodeWithText("Galería").assertIsEnabled()
    }
}
