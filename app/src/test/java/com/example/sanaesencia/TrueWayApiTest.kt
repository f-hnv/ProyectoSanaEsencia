package com.example.sanaesencia

import com.example.sanaesencia.data.remote.ConexionTrueWay
import com.example.sanaesencia.data.repository.*
import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*

class TrueWayApiTest {
    private lateinit var server: MockWebServer
    private lateinit var repo: RutasRepository
    @Before fun preparar() {
        server = MockWebServer(); server.start()
        repo = RutasRepository(ConexionTrueWay.crear(server.url("/").toString())) { "clave-solo-prueba" }
    }
    @After fun cerrar() { server.shutdown() }
    @Test fun rutaChilenaUsaCoordenadasYNoCredencialesDeUsuarios() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"route":{"distance":1825,"duration":369,"geometry":{}}}"""))
        val ruta = repo.consultar(-31.9,-71.5)
        assertEquals(1825.0,ruta.metros,0.0); assertEquals("7 min",ruta.duracionTexto)
        val request = server.takeRequest()
        assertEquals("/FindDrivingRoute",request.requestUrl!!.encodedPath)
        assertEquals("-31.9,-71.5;-31.909825,-71.512501",request.requestUrl!!.queryParameter("stops"))
        assertEquals("clave-solo-prueba",request.getHeader("X-RapidAPI-Key"))
        assertEquals("trueway-directions2.p.rapidapi.com",request.getHeader("X-RapidAPI-Host"))
        assertNull(request.getHeader("Authorization")); assertEquals("GET",request.method)
    }
    @Test fun sinClaveNoHacePeticiones() = runBlocking {
        val sinClave = RutasRepository(ConexionTrueWay.crear(server.url("/").toString())) { "" }
        try { sinClave.consultar(-31.9,-71.5); fail() } catch(e:ErrorRuta) { assertTrue(e.message.contains("mapas")) }
        assertEquals(0,server.requestCount)
    }
    @Test fun coordenadasInvalidasNoSeEnvian() = runBlocking {
        for ((lat,lon) in listOf(Double.NaN to -71.0,91.0 to 0.0,0.0 to 181.0)) {
            try { repo.consultar(lat,lon); fail() } catch(e:ErrorRuta) { assertTrue(e.message.contains("ubicación")) }
        }
        assertEquals(0,server.requestCount)
    }
    @Test fun rutaAusenteOIncompletaNoInventaDistancia() = runBlocking {
        for(body in listOf("{}","""{"route":null}""","""{"route":{"distance":100}}""",
            """{"route":{"distance":-1,"duration":5}}""")) {
            server.enqueue(MockResponse().setBody(body))
            try { repo.consultar(-31.9,-71.5); fail() } catch(e:ErrorRuta) { assertTrue(e.message.contains("recorrido")) }
        }
    }
    @Test fun erroresNoRevelanClaveNiRespuestaInterna() = runBlocking {
        for(code in listOf(401,403,429,503)) {
            server.enqueue(MockResponse().setResponseCode(code).setBody("clave-solo-prueba secreto"))
            try { repo.consultar(-31.9,-71.5); fail() } catch(e:ErrorRuta) {
                assertFalse(e.message.contains("clave-solo-prueba")); assertFalse(e.message.contains("secreto"))
                if(code == 429) assertTrue(e.message.contains("límite"))
            }
        }
    }
    @Test fun jsonMalformadoEsUnErrorRecuperable() = runBlocking {
        server.enqueue(MockResponse().setBody("{invalido"))
        try { repo.consultar(-31.9,-71.5); fail() } catch(e:ErrorRuta) { assertTrue(e.message.isNotBlank()) }
    }
    @Test fun respuestaSinContenidoNoProvocaUnCierre() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        try { repo.consultar(-31.9,-71.5); fail() } catch(e:ErrorRuta) { assertTrue(e.message.contains("recorrido")) }
    }
    @Test fun cancelacionNoSeConvierteEnErrorDeConexion() = runBlocking {
        val cancelado = RutasRepository(object: com.example.sanaesencia.data.remote.TrueWayApi {
            override suspend fun ruta(puntos:String,clave:String):retrofit2.Response<com.example.sanaesencia.data.remote.RutaTrueWayRespuesta> =
                throw CancellationException("cancelado")
        }) { "prueba" }
        try { cancelado.consultar(-31.9,-71.5); fail() } catch(_:CancellationException) { }
    }
    @Test fun muestraMetrosKilometrosYHorasSinTruncarDuracion() {
        assertEquals("850 m",RecorridoSede(850.0,60.0).distanciaTexto)
        assertEquals("1,8 km",RecorridoSede(1825.0,369.0).distanciaTexto)
        assertEquals("1 h 1 min",RecorridoSede(50000.0,3601.0).duracionTexto)
    }
}
