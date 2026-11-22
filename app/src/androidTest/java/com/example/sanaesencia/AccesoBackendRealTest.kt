package com.example.sanaesencia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.sanaesencia.data.remote.*
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import java.util.UUID

/** E2E explícito: requiere ms-usuarios y MySQL activos; usa únicamente una cuenta ficticia propia. */
class AccesoBackendRealTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val email="android.e2e.${UUID.randomUUID()}@example.com"
    private val clave="AndroidE2E${UUID.randomUUID().toString().take(12)}!"
    private var id:Long?=null
    private val api=ConexionBackend.crearUsuarios(BuildConfig.USUARIOS_API_URL)
    private val run:String = run {
        val cuerpo=(60_000_000L+System.nanoTime()%9_000_000).toString()
        val suma=cuerpo.reversed().mapIndexed { i,c -> c.digitToInt()*(2+i%6) }.sum()
        val dv=when(val r=11-suma%11) { 11 -> "0"; 10 -> "K"; else -> r.toString() }
        "$cuerpo-$dv"
    }
    interface LimpiezaApi {
        @DELETE("api/usuarios/{id}") suspend fun desactivar(@Path("id") id:Long,@Header("Authorization") auth:String)
    }
    @After fun limpiar() = runBlocking {
        val cuenta = id ?: runCatching { api.login(LoginRequest(email,clave)).id }.getOrNull()
        try {
            if(cuenta!=null) Retrofit.Builder().baseUrl(BuildConfig.USUARIOS_API_URL)
                .addConverterFactory(GsonConverterFactory.create()).build().create(LimpiezaApi::class.java)
                .desactivar(cuenta,Credentials.basic(email,clave,Charsets.UTF_8))
        } finally { SesionBackend.cerrar() }
    }
    @Test fun registroLoginPerfilYLogoutContraMySql() {
        compose.onNodeWithText("¿No tienes cuenta? Regístrate aquí").performScrollTo().performClick()
        compose.onNodeWithText("Nombre").performScrollTo().performTextInput("Paciente")
        compose.onNodeWithText("Apellido").performScrollTo().performTextInput("Android E2E")
        compose.onNodeWithText("RUN").performScrollTo().performTextInput(run)
        compose.onNodeWithText("Correo Electrónico").performScrollTo().performTextInput(email)
        compose.onNodeWithText("Contraseña").performScrollTo().performTextInput(clave)
        compose.onNodeWithText("Confirmar Contraseña").performScrollTo().performTextInput(clave)
        compose.onNodeWithText("Registrarse").performScrollTo().performClick()
        try {
            compose.waitUntil(30_000) { compose.onAllNodesWithText("Ingresar").fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            fun textos(nodo:SemanticsNode):List<String> =
                nodo.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } + nodo.children.flatMap(::textos)
            throw AssertionError("El registro no terminó: ${textos(compose.onRoot(useUnmergedTree=true).fetchSemanticsNode())}",e)
        }
        compose.onNodeWithText("Correo Electrónico").performTextInput(email)
        compose.onNodeWithText("Contraseña").performTextInput(clave)
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("nav-home").fetchSemanticsNodes().isNotEmpty() }
        val cuenta=SesionBackend.actual()!!.usuario
        id=cuenta.id
        assertEquals("PACIENTE",cuenta.rol); assertEquals(run,cuenta.run); assertEquals(email,cuenta.correo)
        val app=androidx.test.core.app.ApplicationProvider.getApplicationContext<SanaesenciaApp>()
        compose.onNodeWithTag("nav-catalogo_servicios").performClick()
        compose.waitUntil(30_000) { app.container.catalogoRepository.estado.value.vigente }
        val catalogo=app.container.catalogoRepository
        val snapshot=catalogo.estado.value
        val (servicioRemoto,profesionalRemoto)=runBlocking {
            val servicios=ConexionBackend.catalogo.servicios()
            val perfiles=ConexionBackend.catalogo.especialistas()
            val profesional=perfiles.first { p -> p.servicios.any { s -> servicios.any { it.id==s.id } } }
            servicios.first { s -> profesional.servicios.any { it.id==s.id } } to profesional
        }
        val servicioLocal=snapshot.servicios.first { it.nombre==servicioRemoto.nombre }
        val perfilLocal=snapshot.especialistas.first { it.nombre==profesionalRemoto.nombre }
        runBlocking {
            assertEquals(servicioRemoto.id,catalogo.idServicioServidor(servicioLocal.id))
            assertEquals(profesionalRemoto.id,catalogo.idEspecialistaServidor(perfilLocal.id))
        }
        assertEquals(servicioRemoto.duracionMinutos,servicioLocal.duracionMin)
        assertEquals(0,servicioRemoto.precio.compareTo(servicioLocal.precioServidor!!.toBigDecimal()))
        compose.onNodeWithTag("catalogo").performScrollToNode(hasTestTag("ver-especialistas-${servicioLocal.id}"))
        compose.onNodeWithTag("ver-especialistas-${servicioLocal.id}").performClick()
        compose.onNodeWithTag("catalogo").performScrollToNode(hasTestTag("elegir-${servicioLocal.id}-${perfilLocal.id}"))
        compose.onNodeWithTag("elegir-${servicioLocal.id}-${perfilLocal.id}").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("especialista-agenda-${perfilLocal.id}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("especialista-agenda-${perfilLocal.id}").assertIsSelected()
        compose.onNodeWithText("Confirmar Reserva").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("nav-perfil").performClick()
        compose.onNodeWithText("Nombre").performScrollTo().performTextReplacement("Paciente Editado")
        compose.onNodeWithText("Guardar perfil").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Cambios guardados").fetchSemanticsNodes().isNotEmpty() }
        runBlocking {
            val leida=api.obtener(cuenta.id,Credentials.basic(email,clave,Charsets.UTF_8))
            assertEquals("Paciente Editado",leida.nombre); assertEquals("Android E2E",leida.apellido)
        }
        compose.onNodeWithText("Cerrar Sesión").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Ingresar").fetchSemanticsNodes().isNotEmpty() }
        assertNull(SesionBackend.actual())
    }
}
