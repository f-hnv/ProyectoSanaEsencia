package com.example.sanaesencia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.utils.FechasReservas
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.Path
import java.util.UUID

/** Opt-in: prueba HTTPS real; crea su propia cuenta temporal y cancela sus citas. */
class ReservasAwsTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val correo="aws.android.${UUID.randomUUID()}@example.com"
    private val clave="AwsE2E${UUID.randomUUID()}!"
    private val auth get()=Credentials.basic(correo,clave,Charsets.UTF_8)
    private var cuentaId:Long?=null
    private var reservaId:Long?=null
    private var mesPrueba:String?=null
    private val api=ConexionBackend.reservas
    interface LimpiezaApi {
        @DELETE("api/usuarios/{id}") suspend fun desactivar(@Path("id") id:Long,@Header("Authorization") auth:String)
    }
    @Before fun preparar()=runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("awsE2e")=="true")
        assertTrue(BuildConfig.USUARIOS_API_URL.startsWith("https://"))
        assertTrue(BuildConfig.CATALOGO_API_URL.startsWith("https://"))
        assertTrue(BuildConfig.RESERVAS_API_URL.startsWith("https://"))
        val cuerpo=(70_000_000L+UUID.randomUUID().mostSignificantBits.and(Long.MAX_VALUE)%9_000_000).toString()
        val suma=cuerpo.reversed().mapIndexed {i,c->c.digitToInt()*(2+i%6)}.sum()
        val dv=when(val resto=11-suma%11){11->"0";10->"K";else->resto.toString()}
        cuentaId=ConexionBackend.usuarios.registrar(RegistroRequest("Paciente","AWS E2E","$cuerpo-$dv",correo,clave)).id
    }
    @After fun limpiar()=runBlocking {
        try {
            val creadas=mesPrueba?.let {mes->api.listar(mes,auth).map {it.id}} ?: listOfNotNull(reservaId)
            creadas.forEach { id->
                val r=api.obtener(id,auth)
                if(r.estado=="CONFIRMADA")api.cancelar(id,auth)
                assertEquals("CANCELADA",api.obtener(id,auth).estado)
            }
        } finally {
            try {
                cuentaId?.let { id-> Retrofit.Builder().baseUrl(BuildConfig.USUARIOS_API_URL)
                    .addConverterFactory(GsonConverterFactory.create()).build().create(LimpiezaApi::class.java)
                    .desactivar(id,auth) }
            } finally { SesionBackend.cerrar() }
        }
    }
    @Test fun loginReservaPresencialReprogramacionOnlineYCancelacionEnAws() {
        val profesional=runBlocking {ConexionBackend.catalogo.especialistas().single {it.nombre=="Samantha Bastías"}}
        val servicio=profesional.servicios.first {it.activo && it.permiteOnline}
        val opciones=runBlocking {
            FechasReservas.dias(cantidad=7).firstNotNullOfOrNull {dia->
                val horas=api.disponibilidad(profesional.id,servicio.id,dia,Modalidad.PRESENCIAL,null,auth)
                    .filter {FechasReservas.instante(it.inicio)-System.currentTimeMillis()>13*60*60*1000L}
                horas.firstOrNull()?.let {primera->horas.firstOrNull {FechasReservas.instante(it.inicio)>=FechasReservas.instante(primera.fin)}
                    ?.let {segunda->Triple(dia,primera.inicio,segunda.inicio)}}
            } ?: error("No hay dos cupos publicados futuros para probar sin cambiar la agenda")
        }
        mesPrueba=opciones.first.take(7)
        compose.waitUntil(25_000){runCatching {compose.onAllNodesWithText("Ingresar").fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
        compose.onNodeWithText("Correo Electrónico").performTextReplacement(correo)
        compose.onNodeWithText("Contraseña").performTextReplacement(clave)
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.waitUntil(30_000){compose.onAllNodesWithTag("nav-home").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(cuentaId,SesionBackend.actual()!!.usuario.id)
        compose.onNodeWithTag("nav-catalogo_servicios").performClick()
        val app=ApplicationProvider.getApplicationContext<SanaesenciaApp>()
        val catalogo=app.container.catalogoRepository
        compose.waitUntil(30_000){catalogo.estado.value.vigente}
        val local=catalogo.estado.value.servicios.single {it.nombre==servicio.nombre}
        val perfilLocal=runBlocking {catalogo.idEspecialistaLocal(profesional.id)!!}
        compose.onNodeWithTag("catalogo").performScrollToNode(hasTestTag("ver-especialistas-${local.id}"))
        compose.onNodeWithTag("ver-especialistas-${local.id}").performClick()
        compose.onNodeWithTag("catalogo").performScrollToNode(hasTestTag("elegir-${local.id}-$perfilLocal"))
        compose.onNodeWithTag("elegir-${local.id}-$perfilLocal").performClick()
        compose.waitUntil(25_000){compose.onAllNodesWithTag("fecha-${opciones.first}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("fecha-${opciones.first}").performScrollTo().performClick()
        compose.waitUntil(25_000){compose.onAllNodesWithTag("turno-${opciones.second}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("turno-${opciones.second}").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar Reserva").performScrollTo().performClick()
        compose.waitUntil(30_000){compose.onAllNodesWithTag("recibo-reserva").fetchSemanticsNodes().isNotEmpty()}
        val creada=runBlocking {api.listar(opciones.first.take(7),auth).single {it.estado=="CONFIRMADA"}}
        reservaId=creada.id
        assertEquals(profesional.id,creada.especialistaId)
        assertEquals(Modalidad.PRESENCIAL,creada.modalidad)
        assertNotNull(creada.boxId);assertFalse(creada.boxNombre.isNullOrBlank())
        compose.onNodeWithTag("box-confirmado").assertTextEquals("Presencial · ${creada.boxNombre}")
        compose.onNodeWithText("Ver mis citas").performClick()
        compose.waitUntil(25_000){compose.onAllNodesWithTag("reserva-${creada.id}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Modificar").performScrollTo().performClick()
        compose.waitUntil(25_000){compose.onAllNodesWithText("Online").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Online",useUnmergedTree=true).performScrollTo().performClick()
        compose.waitUntil(25_000){compose.onAllNodesWithTag("turno-${opciones.third}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("turno-${opciones.third}").performScrollTo().performClick()
        compose.onNodeWithText("Guardar Cambios").performScrollTo().performClick()
        compose.waitUntil(30_000){compose.onAllNodesWithTag("recibo-reserva").fetchSemanticsNodes().isNotEmpty()}
        val cambiada=runBlocking {api.obtener(creada.id,auth)}
        assertEquals(opciones.third,cambiada.inicio);assertEquals(Modalidad.ONLINE,cambiada.modalidad);assertNull(cambiada.boxId)
        compose.onNodeWithText("Ver mis citas").performClick()
        compose.waitUntil(25_000){compose.onAllNodesWithTag("reserva-${creada.id}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Cancelar",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Confirmar",substring=false).performClick()
        compose.waitUntil(25_000){runBlocking {api.obtener(creada.id,auth).estado=="CANCELADA"}}
        val libres=runBlocking {api.disponibilidad(profesional.id,servicio.id,opciones.first,Modalidad.PRESENCIAL,null,auth)}
        assertTrue(libres.any {it.inicio==opciones.third})
    }
}
