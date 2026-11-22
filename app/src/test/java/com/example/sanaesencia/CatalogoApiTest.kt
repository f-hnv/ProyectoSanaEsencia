package com.example.sanaesencia

import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.local.entity.ServicioEntity
import com.example.sanaesencia.utils.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import java.math.BigDecimal

class CatalogoApiTest {
    private lateinit var server:MockWebServer
    private lateinit var api:CatalogoApi
    private val servicio="""{"id":700,"nombre":"Servicio remoto","descripcion":"Descripción","precio":34567.50,"duracionMinutos":75,"permiteOnline":false,"activo":true}"""
    @Before fun preparar() { server=MockWebServer(); server.start(); api=ConexionBackend.crearCatalogo(server.url("/").toString()) }
    @After fun cerrar() { server.shutdown() }
    @Test fun cargaPrecioDecimalDuracionEIdentidadDelServidor() = runBlocking {
        server.enqueue(MockResponse().setBody("[$servicio]"))
        val s=api.servicios().single()
        assertEquals(700L,s.id); assertEquals(BigDecimal("34567.50"),s.precio); assertEquals(75,s.duracionMinutos)
        assertFalse(s.permiteOnline)
        val request=server.takeRequest(); assertEquals("/api/servicios",request.path); assertEquals("GET",request.method)
        assertNull(request.getHeader("Authorization"))
    }
    @Test fun especialistaConservaServiciosYModalidadesSinInferirPorNombre() = runBlocking {
        server.enqueue(MockResponse().setBody("""[{"id":900,"nombre":"Nombre","especialidad":"Especialidad","descripcion":"Perfil","permitePresencial":true,"permiteOnline":false,"activo":true,"servicios":[$servicio]}]"""))
        val p=api.especialistas(700).single()
        assertEquals(900L,p.id); assertEquals(700L,p.servicios.single().id); assertFalse(p.permiteOnline)
        assertEquals("/api/especialistas?servicioId=700",server.takeRequest().path)
    }
    @Test fun consultaDetalleConIdRemoto() = runBlocking {
        server.enqueue(MockResponse().setBody(servicio)); api.servicio(700)
        assertEquals("/api/servicios/700",server.takeRequest().path)
        server.enqueue(MockResponse().setBody("""{"id":900,"nombre":"Nombre","especialidad":"Especialidad","descripcion":"Perfil","permitePresencial":true,"permiteOnline":true,"activo":true,"servicios":[$servicio]}"""))
        api.especialista(900); assertEquals("/api/especialistas/900",server.takeRequest().path)
    }
    @Test fun listaVaciaNoSeLlenaConServiciosFicticios() = runBlocking {
        server.enqueue(MockResponse().setBody("[]")); assertTrue(api.servicios().isEmpty())
    }
    @Test fun erroresDeServidorSonRecuperables() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503).setBody("<html>interno</html>"))
        try { llamadaBackend { api.servicios() }; fail() } catch(e:ErrorBackend) { assertEquals(503,e.estado); assertFalse(e.message.contains("interno")) }
    }
    @Test fun jsonInvalidoNoSeConvierteEnCatalogoVacio() = runBlocking {
        server.enqueue(MockResponse().setBody("{invalido"))
        try { llamadaBackend { api.servicios() }; fail() } catch(e:ErrorBackend) { assertTrue(e.message.isNotBlank()) }
    }
    @Test fun precioExactoNoSeTruncaNiReemplazaPorElValorLocalAnterior() {
        val s=ServicioEntity(1,"Servicio","Descripción","Centro",75,34568,precioServidor="34567.50")
        assertEquals(BigDecimal("34567.50"),s.precioExacto())
        assertTrue(s.precioFormateado().contains("34.567,50"))
        assertTrue(s.copy(precioServidor="30000.00").precioFormateado().contains("30.000"))
        assertEquals(BigDecimal(34568),s.copy(precioServidor=null).precioExacto())
    }
    @Test fun unErrorDeProgramacionNoSeOcultaComoErrorJson() = runBlocking {
        val original=IllegalStateException("Error ajeno a la respuesta HTTP")
        try { llamadaBackend<Unit> { throw original }; fail() }
        catch(e:IllegalStateException) { assertSame(original,e) }
    }
}
