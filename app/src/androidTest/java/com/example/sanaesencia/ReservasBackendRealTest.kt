package com.example.sanaesencia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.utils.FechasReservas
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import org.junit.*
import org.junit.Assert.*

/** Opt-in: cuentas de prueba y horarios publicados de un profesional real, en el backend local. */
class ReservasBackendRealTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var fixture:JsonObject
    private var reservaId:Long?=null
    private val api=ConexionBackend.reservas
    private fun auth(campo:String)=Credentials.basic(fixture[campo].asString,fixture["clave"].asString,Charsets.UTF_8)
    @Before fun preparar() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("reservasE2e")=="true")
        val archivo=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cat /data/local/tmp/sana-reservas-e2e.json")
        fixture=android.os.ParcelFileDescriptor.AutoCloseInputStream(archivo).bufferedReader().use {JsonParser.parseReader(it).asJsonObject}
    }
    private fun entrar(campo:String) {
        compose.waitUntil(20_000){compose.onAllNodesWithText("Ingresar").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Correo Electrónico").performTextReplacement(fixture[campo].asString)
        compose.onNodeWithText("Contraseña").performTextReplacement(fixture["clave"].asString)
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.waitUntil(30_000){compose.onAllNodesWithTag("nav-home").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun salir() {
        compose.onNodeWithTag("nav-perfil").performClick()
        compose.onNodeWithText("Cerrar Sesión").performScrollTo().performClick()
        compose.waitUntil(15_000){compose.onAllNodesWithText("Ingresar").fetchSemanticsNodes().isNotEmpty()}
    }
    @After fun limpiar()=runBlocking {
        if(!::fixture.isInitialized)return@runBlocking
        reservaId?.let { id->runCatching {val r=api.obtener(id,auth("paciente"));if(FechasReservas.cancelable(r))api.cancelar(id,auth("paciente"))} }
        SesionBackend.cerrar()
    }
    @Test fun reservaBoxReprogramacionVistaDelEspecialistaYCancelacionEnMySql() {
        entrar("paciente")
        val app=androidx.test.core.app.ApplicationProvider.getApplicationContext<SanaesenciaApp>()
        compose.onNodeWithTag("nav-catalogo_servicios").performClick()
        compose.waitUntil(30_000){app.container.catalogoRepository.estado.value.vigente}
        val catalogo=app.container.catalogoRepository
        val servicio=catalogo.estado.value.servicios.first {it.nombre==fixture["servicioNombre"].asString}
        val perfil=runBlocking {catalogo.idEspecialistaLocal(fixture["especialistaId"].asLong)!!}
        compose.onNodeWithTag("catalogo").performScrollToNode(hasTestTag("ver-especialistas-${servicio.id}"))
        compose.onNodeWithTag("ver-especialistas-${servicio.id}").performClick()
        compose.onNodeWithTag("catalogo").performScrollToNode(hasTestTag("elegir-${servicio.id}-$perfil"))
        compose.onNodeWithTag("elegir-${servicio.id}-$perfil").performClick()
        compose.waitUntil(30_000){compose.onAllNodesWithTag("especialista-agenda-$perfil").fetchSemanticsNodes().isNotEmpty()}
        val fecha=fixture["fecha"].asString
        compose.onNodeWithTag("fecha-$fecha").performScrollTo().performClick()
        val inicio=fixture["inicio"].asString
        compose.waitUntil(20_000){compose.onAllNodesWithTag("turno-$inicio").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("turno-$inicio").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar Reserva").performScrollTo().performClick()
        compose.waitUntil(30_000){compose.onAllNodesWithTag("recibo-reserva").fetchSemanticsNodes().isNotEmpty()}
        val creada=runBlocking {api.listar(fecha.take(7),auth("paciente")).single {it.estado=="CONFIRMADA"}}
        reservaId=creada.id
        assertNotNull(creada.boxId);assertFalse(creada.boxNombre.isNullOrBlank());assertEquals(Modalidad.PRESENCIAL,creada.modalidad)
        compose.onNodeWithTag("box-confirmado").assertTextEquals("Presencial · ${creada.boxNombre}")
        compose.onNodeWithText("Ver mis citas").performClick()
        compose.waitUntil(20_000){compose.onAllNodesWithTag("reserva-${creada.id}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Modificar").performScrollTo().performClick()
        compose.waitUntil(20_000){compose.onAllNodesWithText("Online").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Online",useUnmergedTree=true).performScrollTo().performClick()
        val nuevo=fixture["nuevoInicio"].asString
        compose.waitUntil(20_000){compose.onAllNodesWithTag("turno-$nuevo").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("turno-$nuevo").performScrollTo().performClick()
        compose.onNodeWithText("Guardar Cambios").performScrollTo().performClick()
        compose.waitUntil(30_000){compose.onAllNodesWithTag("recibo-reserva").fetchSemanticsNodes().isNotEmpty()}
        val cambiada=runBlocking {api.obtener(creada.id,auth("paciente"))}
        assertEquals(creada.id,cambiada.id);assertNull(cambiada.boxId);assertEquals(Modalidad.ONLINE,cambiada.modalidad)
        assertEquals(nuevo,cambiada.inicio)
        assertEquals(1,runBlocking {api.listar(fecha.take(7),auth("paciente")).count {it.estado=="CONFIRMADA"}})
        compose.onNodeWithText("Ver mis citas").performClick()
        salir();entrar("especialista")
        compose.waitUntil(25_000){compose.onAllNodesWithTag("cita-terapeuta-${creada.id}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("cita-terapeuta-${creada.id}").performScrollTo().assertExists()
        val propias=runBlocking {api.listar(fecha.take(7),auth("especialista"))}
        assertTrue(propias.any {it.id==creada.id && it.especialistaId==fixture["especialistaId"].asLong})
        salir();entrar("paciente")
        compose.waitUntil(20_000){compose.onAllNodesWithTag("reserva-${creada.id}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Cancelar",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Confirmar",substring=false).performClick()
        compose.waitUntil(20_000){runBlocking {api.obtener(creada.id,auth("paciente")).estado=="CANCELADA"}}
        val libre=runBlocking {api.disponibilidad(fixture["especialistaId"].asLong,fixture["servicioId"].asLong,fecha,Modalidad.PRESENCIAL,null,auth("paciente"))}
        assertTrue(libre.any {it.inicio==nuevo})
        val lejos=runBlocking {api.sugerencia(fixture["especialistaId"].asLong,fixture["servicioId"].asLong,-33.45,-70.66,auth("paciente"))}
        assertTrue(lejos.sugiereOnline)
    }
}
