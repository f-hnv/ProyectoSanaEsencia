package com.example.sanaesencia

import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.repository.ReservasRepository
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.utils.FechasReservas
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import java.util.concurrent.TimeUnit

class ReservasApiTest {
    private lateinit var server:MockWebServer
    private lateinit var repo:ReservasRepository
    private var sesion:SesionBackend.Sesion?=null
    private val json="""{"id":77,"pacienteId":42,"pacienteNombre":"Paciente Prueba","especialistaId":9,"especialistaNombre":"Profesional Prueba","servicioId":101,"servicioNombre":"Terapia","precio":30000.50,"inicio":"2027-01-15T09:00:00","fin":"2027-01-15T10:00:00","modalidad":"PRESENCIAL","estado":"CONFIRMADA","boxId":12,"boxNombre":"Sala Azul","distanciaKm":25.0,"sugiereOnline":true}"""
    @Before fun antes() {
        server=MockWebServer();server.start()
        sesion=SesionBackend.Sesion(UsuarioRemoto(42,"Paciente","Prueba","12345678-5","prueba@example.com","PACIENTE",null,true),"Basic prueba")
        repo=ReservasRepository(ConexionBackend.crearReservas(server.url("/").toString())) {sesion}
    }
    @After fun despues(){server.shutdown()}
    @Test fun creaConIdsServidorYBoxAsignadoSinIdPacienteEnBody()=runBlocking {
        server.enqueue(MockResponse().setBody(json))
        val r=repo.guardar(null,ReservaSolicitud(101,9,"2027-01-15T09:00:00",Modalidad.PRESENCIAL,-33.0,-71.0))
        assertEquals("Sala Azul",r.boxNombre);assertEquals("30000.50",r.precio.toPlainString())
        val req=server.takeRequest();assertEquals("POST",req.method);assertEquals("/api/reservas",req.path);assertEquals("Basic prueba",req.getHeader("Authorization"))
        val b=JsonParser.parseString(req.body.readUtf8()).asJsonObject
        assertFalse(b.has("pacienteId"));assertFalse(b.has("boxId"));assertEquals(101L,b["servicioId"].asLong)
    }
    @Test fun listaMesReprogramaMismoIdYCancela()=runBlocking {
        server.enqueue(MockResponse().setBody("[$json]"));assertEquals(77L,repo.listar("2027-01").single().id)
        assertEquals("/api/reservas?mes=2027-01",server.takeRequest().path)
        server.enqueue(MockResponse().setBody(json));repo.guardar(77,ReservaSolicitud(101,9,"2027-01-15T09:00:00",Modalidad.PRESENCIAL))
        val edit=server.takeRequest();assertEquals("PUT",edit.method);assertEquals("/api/reservas/77",edit.path)
        server.enqueue(MockResponse().setResponseCode(204));repo.cancelar(77);assertEquals("DELETE",server.takeRequest().method)
    }
    @Test fun disponibilidadExcluyeReservaPropiaYConservaBoxReal()=runBlocking {
        server.enqueue(MockResponse().setBody("""[{"inicio":"2027-01-15T09:00:00","fin":"2027-01-15T10:00:00","boxesDisponibles":[{"id":12,"nombre":"Sala Azul"}]}]"""))
        assertEquals("Sala Azul",repo.disponibilidad(9,101,"2027-01-15",Modalidad.PRESENCIAL,77).single().boxesDisponibles.single().nombre)
        val url=server.takeRequest().requestUrl!!;assertEquals("77",url.queryParameter("reservaId"));assertEquals("9",url.queryParameter("especialistaId"))
    }
    @Test fun onlineNoRequiereNiAsignaBox()=runBlocking {
        server.enqueue(MockResponse().setBody(json.replace("\"PRESENCIAL\"","\"ONLINE\"").replace("\"boxId\":12","\"boxId\":null").replace("\"boxNombre\":\"Sala Azul\"","\"boxNombre\":null")))
        val r=repo.obtener(77);assertNull(r.boxId);assertNull(r.boxNombre);assertEquals(Modalidad.ONLINE,r.modalidad)
    }
    @Test fun sugerenciaSeObtieneDelServidorConGps()=runBlocking {
        server.enqueue(MockResponse().setBody("""{"distanciaKm":25.1,"permitePresencial":true,"permiteOnline":true,"sugiereOnline":true,"mensaje":"Puedes elegir online"}"""))
        assertTrue(repo.sugerencia(9,101,-33.0,-71.0).sugiereOnline)
        assertEquals("-33.0",server.takeRequest().requestUrl!!.queryParameter("latitud"))
    }
    @Test fun conflictoNoSeConvierteEnUnaReservaLocal()=runBlocking {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"mensaje":"El especialista ya tiene una cita"}"""))
        try {repo.guardar(null,ReservaSolicitud(101,9,"2027-01-15T09:00:00",Modalidad.PRESENCIAL));fail()}
        catch(e:ErrorBackend){assertEquals(409,e.estado);assertTrue(e.message.contains("especialista"))}
        assertEquals(1,server.requestCount)
    }
    @Test fun noEntregaCitasDeOtraCuenta()=runBlocking {
        server.enqueue(MockResponse().setBody("["+json.replace("\"pacienteId\":42","\"pacienteId\":99")+"]"))
        try {repo.listar("2027-01");fail()}catch(_:IllegalArgumentException){}
    }
    @Test fun respuestaTardiaNoCruzaUnCambioDeSesion()=runBlocking {
        server.enqueue(MockResponse().setBody("[$json]").setBodyDelay(200,TimeUnit.MILLISECONDS))
        val llamada=async {repo.listar("2027-01")}
        delay(80);sesion=null
        try{llamada.await();fail()}catch(_:CancellationException){}
    }
    @Test fun sinSesionNoLlamaAlServidor()=runBlocking {
        sesion=null
        try{repo.listar("2027-01");fail()}catch(e:ErrorBackend){assertEquals(401,e.estado)}
        assertEquals(0,server.requestCount)
    }
    @Test fun fechasUsanChileYMesesCruzanElAnio() {
        assertEquals("09:00",FechasReservas.mostrar("2027-01-15T09:00:00","HH:mm"))
        assertEquals("2027-01",FechasReservas.mes(com.example.sanaesencia.domain.model.PeriodoAgenda(2026,12).desplazar(1)))
        assertEquals(61,FechasReservas.dias().distinct().size)
    }
}
