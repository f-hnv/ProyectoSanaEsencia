package com.example.sanaesencia

import com.example.sanaesencia.data.remote.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UsuariosApiTest {
    private lateinit var servidor: MockWebServer
    private lateinit var api: UsuariosApi
    @Before fun preparar() { servidor=MockWebServer(); servidor.start(); api=ConexionBackend.crearUsuarios(servidor.url("/").toString()); SesionBackend.cerrar() }
    @After fun cerrar() { servidor.shutdown(); SesionBackend.cerrar() }
    private fun cuenta(rol: String="PACIENTE") = """{"id":42,"nombre":"Ana","apellido":"Pérez","run":"12345678-5","correo":"ana@example.com","rol":"$rol","especialistaId":7,"activo":true}"""

    @Test fun registroEnviaApellidoRunYClaveSinRolNiAutorizacion() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(201).setBody(cuenta()))
        val u=api.registrar(RegistroRequest("Ana","Pérez","12345678-5","ana@example.com","PruebaSana2026!"))
        val peticion=servidor.takeRequest()
        assertEquals("POST",peticion.method); assertEquals("/api/usuarios/registro",peticion.path)
        val body=JsonParser.parseString(peticion.body.readUtf8()).asJsonObject
        assertEquals("Pérez",body["apellido"].asString); assertEquals("12345678-5",body["run"].asString)
        assertEquals("PruebaSana2026!",body["password"].asString)
        assertFalse(body.has("rol")); assertNull(peticion.getHeader("Authorization")); assertEquals(42L,u.id)
    }
    @Test fun loginConservaLosTresRolesDelServidor() = runBlocking {
        for (rol in listOf("PACIENTE","ESPECIALISTA","ADMIN")) {
            servidor.enqueue(MockResponse().setBody(cuenta(rol)))
            val u=llamadaBackend { api.login(LoginRequest("ana@example.com","PruebaSana2026!")) }
            assertEquals(rol,u.rol); assertEquals(7L,u.especialistaId)
            assertEquals("/api/usuarios/login",servidor.takeRequest().path)
        }
    }
    @Test fun perfilUsaIdDelServidorYAutorizacionBasic() = runBlocking {
        servidor.enqueue(MockResponse().setBody(cuenta()))
        val autorizacion=Credentials.basic("ana@example.com","PruebaSana2026!")
        api.editar(42,autorizacion,PerfilRequest("Ana","Pérez"))
        val p=servidor.takeRequest(); assertEquals("PUT",p.method); assertEquals("/api/usuarios/42",p.path)
        assertEquals(autorizacion,p.getHeader("Authorization"))
    }
    @Test fun pacienteEliminaVinculoDeEspecialistaEnCambioDeRol() = runBlocking {
        servidor.enqueue(MockResponse().setBody(cuenta()))
        api.cambiarRol(42,"Basic ejemplo",RolRequest("PACIENTE",null))
        val p=servidor.takeRequest(); assertEquals("PATCH",p.method); assertEquals("/api/admin/usuarios/42/rol",p.path)
        val body=JsonParser.parseString(p.body.readUtf8()).asJsonObject
        assertEquals("PACIENTE",body["rol"].asString)
        assertTrue(!body.has("especialistaId") || body["especialistaId"].isJsonNull)
    }
    @Test fun duplicadosMuestranMensajeDelServidor() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(409).setBody("""{"mensaje":"El RUN ya está registrado"}"""))
        try { llamadaBackend { api.login(LoginRequest("ana@example.com","x")) }; fail() }
        catch(e:ErrorBackend) { assertEquals(409,e.estado); assertEquals("El RUN ya está registrado",e.message) }
    }
    @Test fun erroresHtmlNoSeExponenAlUsuario() = runBlocking {
        for (codigo in listOf(400,401,403,404,500,503)) {
            servidor.enqueue(MockResponse().setResponseCode(codigo).setBody("<html>StackTrace SECRETO</html>"))
            try { llamadaBackend { api.login(LoginRequest("ana@example.com","x")) }; fail() }
            catch(e:ErrorBackend) { assertEquals(codigo,e.estado); assertFalse(e.message.contains("SECRETO")); assertFalse(e.message.contains("<html>")) }
        }
    }
    @Test fun caidaDeRedMuestraErrorRecuperable() = runBlocking {
        try { llamadaBackend<Unit> { throw java.io.IOException("datos internos") }; fail() }
        catch(e:ErrorBackend) { assertNull(e.estado); assertTrue(e.message.contains("conectar")); assertFalse(e.message.contains("internos")) }
    }
    @Test fun cancelacionNoSeConvierteEnErrorDeConexion() = runBlocking {
        try { llamadaBackend<Unit> { throw CancellationException("cancelado") }; fail() }
        catch(e:CancellationException) { assertEquals("cancelado",e.message) }
    }
    @Test fun sesionNoExisteHastaLoginYSalirEliminaCredenciales() {
        assertNull(SesionBackend.actual())
        val u=UsuarioRemoto(42,"Ana","Pérez","12345678-5","ana@example.com","PACIENTE",null,true)
        SesionBackend.abrir(u,"PruebaSana2026!")
        assertEquals(Credentials.basic(u.correo,"PruebaSana2026!",Charsets.UTF_8),SesionBackend.actual()?.autorizacion)
        SesionBackend.cerrar(); assertNull(SesionBackend.actual())
    }
    @Test fun cuentasInactivasORolesDesconocidosNoAbrenSesion() {
        val u=UsuarioRemoto(42,"Ana","Pérez","12345678-5","ana@example.com","PACIENTE",null,false)
        for (invalida in listOf(u,u.copy(activo=true,rol="OTRO"),u.copy(activo=true,id=0))) {
            assertThrows(IllegalArgumentException::class.java) { SesionBackend.abrir(invalida,"x") }
            assertNull(SesionBackend.actual())
        }
    }
}
