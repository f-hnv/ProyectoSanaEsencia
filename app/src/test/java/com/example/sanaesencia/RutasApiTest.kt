package com.example.sanaesencia

import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.repository.*
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*

class RutasApiTest {
    private lateinit var server:MockWebServer
    private lateinit var repo:RutasRepository
    @Before fun preparar(){
        server=MockWebServer();server.start()
        repo=RutasRepository(ConexionRutas.crear(server.url("/reservas/").toString())) {"Basic sesion-fixture"}
    }
    @After fun cerrar(){server.shutdown()}
    @Test fun enviaUbicacionEnBodyAlBackendSinClaveExternaNiDestinoManipulable()=runBlocking {
        server.enqueue(MockResponse().setBody("""{"route":{"distance":1825,"duration":369}}"""))
        val ruta=repo.consultar(-31.9,-71.5)
        assertEquals(1825.0,ruta.metros,0.0);assertEquals("7 min",ruta.duracionTexto)
        val r=server.takeRequest()
        assertEquals("/reservas/api/rutas/sede",r.path);assertEquals("POST",r.method)
        assertEquals("Basic sesion-fixture",r.getHeader("Authorization"))
        assertNull(r.getHeader("X-RapidAPI-Key"));assertNull(r.getHeader("X-RapidAPI-Host"))
        val cuerpo=JsonParser.parseString(r.body.readUtf8()).asJsonObject
        assertEquals(setOf("latitud","longitud"),cuerpo.keySet())
        assertEquals(-31.9,cuerpo["latitud"].asDouble,0.0);assertEquals(-71.5,cuerpo["longitud"].asDouble,0.0)
        assertNull(r.requestUrl!!.query)
    }
    @Test fun sinSesionNoHacePeticiones()=runBlocking {
        val anonimo=RutasRepository(ConexionRutas.crear(server.url("/").toString())) {null}
        try{anonimo.consultar(-31.9,-71.5);fail()}catch(e:ErrorRuta){assertTrue(e.message.contains("sesión"))}
        assertEquals(0,server.requestCount)
    }
    @Test fun coordenadasInvalidasNoSeEnvian()=runBlocking {
        for((lat,lon) in listOf(Double.NaN to -71.0,91.0 to 0.0,0.0 to 181.0)) {
            try{repo.consultar(lat,lon);fail()}catch(e:ErrorRuta){assertTrue(e.message.contains("ubicación"))}
        }
        assertEquals(0,server.requestCount)
    }
    @Test fun rutaAusenteOIncompletaNoInventaDistancia()=runBlocking {
        for(cuerpo in listOf("{}","""{"route":null}""","""{"route":{"distance":100}}""","""{"route":{"distance":-1,"duration":5}}""")) {
            server.enqueue(MockResponse().setBody(cuerpo))
            try{repo.consultar(-31.9,-71.5);fail()}catch(e:ErrorRuta){assertTrue(e.message.contains("recorrido"))}
        }
    }
    @Test fun erroresNoRevelanRespuestaInterna()=runBlocking {
        for(codigo in listOf(401,403,429,503)) {
            server.enqueue(MockResponse().setResponseCode(codigo).setBody("credencial secreta del servidor"))
            try{repo.consultar(-31.9,-71.5);fail()}catch(e:ErrorRuta){
                assertFalse(e.message.contains("secreta"));if(codigo==429)assertTrue(e.message.contains("límite"))
            }
        }
    }
    @Test fun jsonMalformadoORespuestaVaciaSonRecuperables()=runBlocking {
        for(respuesta in listOf(MockResponse().setBody("{invalido"),MockResponse().setResponseCode(204))) {
            server.enqueue(respuesta)
            try{repo.consultar(-31.9,-71.5);fail()}catch(e:ErrorRuta){assertTrue(e.message.isNotBlank())}
        }
    }
    @Test fun cancelacionNoSeConvierteEnErrorDeConexion()=runBlocking {
        val cancelado=RutasRepository(object:RutasApi {
            override suspend fun ruta(origen:OrigenRuta,autorizacion:String):retrofit2.Response<RutaSedeRespuesta> = throw CancellationException("cancelado")
        }) {"Basic fixture"}
        try{cancelado.consultar(-31.9,-71.5);fail()}catch(_:CancellationException){}
    }
    @Test fun respuestaTardiaNoCruzaUnCambioDeSesion()=runBlocking {
        var sesion:String?="Basic sesion-a"
        val pendiente=CompletableDeferred<retrofit2.Response<RutaSedeRespuesta>>()
        val repositorio=RutasRepository(object:RutasApi {
            override suspend fun ruta(origen:OrigenRuta,autorizacion:String)=pendiente.await()
        }) {sesion}
        val consulta=async{repositorio.consultar(-31.9,-71.5)}
        yield();sesion=null;pendiente.complete(retrofit2.Response.success(RutaSedeRespuesta(RutaSedeDto(10.0,20.0))))
        try{consulta.await();fail()}catch(_:CancellationException){}
    }
    @Test fun muestraMetrosKilometrosYHorasSinTruncarDuracion(){
        assertEquals("850 m",RecorridoSede(850.0,60.0).distanciaTexto)
        assertEquals("1,8 km",RecorridoSede(1825.0,369.0).distanciaTexto)
        assertEquals("1 h 1 min",RecorridoSede(50000.0,3601.0).duracionTexto)
    }
}
