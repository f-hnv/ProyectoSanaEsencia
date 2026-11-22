package com.example.sanaesencia

import androidx.test.core.app.ApplicationProvider
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.repository.CuentaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CuentaBackendTest {
    @get:Rule val backend=BackendUsuariosFixture()
    private val context get()=ApplicationProvider.getApplicationContext<SanaesenciaApp>()
    @Test fun cacheNoConcedeRolAdminNiAbreSesion() = runBlocking {
        val repo=CuentaRepository(context)
        assertNull(repo.actual())
        repo.entrar("paciente.fixture@example.com",backend.clave)
        val guardada=repo.db.centroDao().usuario(repo.emailActual)!!
        assertEquals("",guardada.hash); assertEquals("",guardada.sal)
        repo.db.centroDao().actualizarUsuario(guardada.copy(rol="ADMIN"))
        assertEquals("PACIENTE",repo.actual()!!.rol)
        repo.salir(); assertNull(repo.actual()); assertEquals("",repo.emailActual)
        assertNull(context.getSharedPreferences("sanaesencia_prefs",0).getString("email_sesion",null))
    }
    @Test fun sinConexionNoUsaCredencialesLocales() = runBlocking {
        val repo=CuentaRepository(context)
        repo.entrar("paciente.fixture@example.com",backend.clave); repo.salir()
        val sinRed=object:UsuariosApi by context.container.usuariosApi {
            override suspend fun login(solicitud:LoginRequest):UsuarioRemoto=throw java.io.IOException("offline")
        }
        try { CuentaRepository(context,sinRed).entrar("paciente.fixture@example.com",backend.clave); fail() }
        catch(e:ErrorBackend) { assertNull(e.estado) }
        assertNull(SesionBackend.actual()); assertNull(repo.actual())
    }
    @Test fun perfilConservaFotoYTelefonoLocalesSinAlterarIdentidadRemota() = runBlocking {
        val repo=CuentaRepository(context)
        repo.entrar("paciente.fixture@example.com",backend.clave)
        repo.editarPerfil("Paciente","+56 9 1234 5678","Editado")
        assertEquals("Paciente",backend.cuentas[42]?.nombre)
        assertEquals("Editado",backend.cuentas[42]?.apellido)
        assertEquals("+56 9 1234 5678",repo.actual()?.telefono)
        assertEquals("PACIENTE",repo.actual()?.rol)
    }
}
