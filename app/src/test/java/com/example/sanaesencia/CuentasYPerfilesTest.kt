package com.example.sanaesencia

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.UsuarioEntity
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.repository.*
import com.example.sanaesencia.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE,application=Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CuentasYPerfilesTest {
    private lateinit var app:Application
    private lateinit var db:AppDatabase
    private lateinit var api:UsuariosApi
    private lateinit var perfiles:PerfilesApi
    private lateinit var repo:CuentaRepository
    private val dispatcher=UnconfinedTestDispatcher()
    private val modelos=mutableListOf<ViewModel>()
    private val usuario=UsuarioRemoto(42,"Ana","Prueba","12345678-5","ana@example.com","PACIENTE",null,true)
    private fun <T:ViewModel> modelo(vm:T)=vm.also {modelos+=it}
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher);app=RuntimeEnvironment.getApplication()
        db=Room.inMemoryDatabaseBuilder(app,AppDatabase::class.java).allowMainThreadQueries().build()
        mockkObject(AppDatabase.Companion);every {AppDatabase.getInstance(any())} returns db
        api=mockk();perfiles=mockk();mockkObject(ConexionBackend)
        every {ConexionBackend.usuarios} returns api;every {ConexionBackend.perfiles} returns perfiles
        coEvery {api.registrar(any())} returns usuario
        coEvery {api.login(any())} returns usuario
        coEvery {api.obtener(42,any())} returns usuario
        coEvery {api.editar(42,any(),any())} answers {usuario.copy(nombre=thirdArg<PerfilRequest>().nombre,apellido=thirdArg<PerfilRequest>().apellido)}
        repo=CuentaRepository(app,api);SesionBackend.cerrar()
    }
    @After fun limpiar() { modelos.forEach {it.viewModelScope.cancel()};SesionBackend.cerrar();unmockkAll();db.close();Dispatchers.resetMain() }
    private suspend fun esperar(condicion:()->Boolean) {
        repeat(200){dispatcher.scheduler.advanceUntilIdle();if(condicion())return;delay(10)}
        assertTrue("No se alcanzó el estado esperado",condicion())
    }
    @Test fun registroNormalizaDatosYRechazaRunYClaveInvalidosAntesDeHttp()=runBlocking {
        repo.registrar(" Ana "," ANA@example.com ","Password2026!"," Prueba ","12345678-5")
        coVerify {api.registrar(match {it.nombre=="Ana" && it.correo=="ana@example.com" && it.apellido=="Prueba"})}
        try {repo.registrar("Ana","ana@example.com","x","Prueba","12345678-5");fail()}catch(_:IllegalArgumentException){}
        try {repo.registrar("Ana","ana@example.com","Password2026!","Prueba","12345678-0");fail()}catch(_:IllegalArgumentException){}
        coVerify(exactly=1){api.registrar(any())}
    }
    @Test fun loginGuardaCopiaSinPasswordYLogoutLimpiaSesionYCredencialesAntiguas()=runBlocking {
        val prefs=app.getSharedPreferences("sanaesencia_prefs",Context.MODE_PRIVATE)
        prefs.edit().putString("password_registrada","antigua").putString("email_sesion","antigua").commit()
        val cache=repo.entrar(" ANA@example.com ","Password2026!")
        assertEquals("",cache.hash);assertEquals("",cache.sal);assertEquals(usuario.id,SesionBackend.actual()!!.usuario.id)
        assertEquals(usuario.correo,repo.emailActual);assertEquals("Ana Prueba",repo.actual()!!.nombre)
        assertEquals(usuario.correo,repo.observarActual().first()!!.email)
        repo.salir();assertNull(SesionBackend.actual());assertFalse(prefs.contains("password_registrada"));assertFalse(prefs.contains("email_sesion"))
        assertNull(repo.actual());repo.limpiarCredencialesAnteriores()
    }
    @Test fun loginRechazaIdentidadInconsistenteYCuentaDesactivada()=runBlocking {
        coEvery {api.login(any())} returns usuario.copy(correo="otra@example.com")
        try{repo.entrar(usuario.correo,"Password2026!");fail()}catch(_:IllegalArgumentException){}
        assertNull(SesionBackend.actual())
        coEvery {api.login(any())} returns usuario.copy(activo=false)
        try{repo.entrar(usuario.correo,"Password2026!");fail()}catch(_:IllegalArgumentException){}
    }
    @Test fun perfilEditadoPersisteNombreTelefonoYFotoLocalSinCambiarLaCuenta()=runBlocking {
        repo.entrar(usuario.correo,"Password2026!")
        repo.editarPerfil("Ana María","+56 9 1234 5678","Prueba")
        assertEquals("Ana María Prueba",repo.actual()!!.nombre);assertEquals("+56 9 1234 5678",repo.actual()!!.telefono)
        val foto=File(app.cacheDir,"foto-prueba.jpg").apply {writeBytes(byteArrayOf(1,2,3))}
        repo.guardarFoto(Uri.fromFile(foto));assertNotNull(repo.actual()!!.fotoUri)
        try{repo.editarPerfil("Ana","abc","Prueba");fail()}catch(_:IllegalArgumentException){}
    }
    @Test fun soloAdminPuedeAsignarEspecialistaConPerfilActivo()=runBlocking {
        SesionBackend.abrir(usuario,"Password2026!")
        try{repo.asignarRol(42,"ESPECIALISTA",9);fail()}catch(_:IllegalArgumentException){}
        val admin=usuario.copy(id=1,correo="admin@example.com",rol="ADMIN")
        SesionBackend.abrir(admin,"Password2026!")
        coEvery {perfiles.obtener(9,any())} returns PerfilProfesionalRemoto(9,"Profesional",true)
        coEvery {api.cambiarRol(42,any(),any())} returns usuario.copy(rol="ESPECIALISTA",especialistaId=9)
        assertEquals(9L,repo.asignarRol(42,"ESPECIALISTA",9).especialistaId)
        coVerify {api.cambiarRol(42,any(),RolRequest("ESPECIALISTA",9))}
        coEvery {perfiles.obtener(9,any())} returns PerfilProfesionalRemoto(9,"Profesional",false)
        try{repo.asignarRol(42,"ESPECIALISTA",9);fail()}catch(_:IllegalArgumentException){}
        assertEquals(42L,repo.buscarUsuario(42).id)
    }
    @Test fun registroViewModelValidaFormularioLimpiaClaveYPresentaErrorDelServidor()=runBlocking {
        val vm=modelo(RegistroViewModel());var registrado=false
        vm.registrar(app){registrado=true};assertFalse(registrado);assertNotNull(vm.nombreError.value)
        vm.onNombreChange("Ana");vm.onApellidoChange("Prueba");vm.onRunChange("12345678-5");vm.onEmailChange(usuario.correo)
        vm.onPasswordChange("Password2026!");vm.onConfirmPasswordChange("otra");assertNotNull(vm.confirmPasswordError.value)
        vm.onConfirmPasswordChange("Password2026!");vm.registrar(app){registrado=true};esperar {registrado}
        assertEquals("",vm.password.value);assertEquals("",vm.confirmPassword.value);assertFalse(vm.cargando.value)
        vm.onPasswordChange("Password2026!");vm.onConfirmPasswordChange("Password2026!")
        coEvery {api.registrar(any())} throws ErrorBackend(409,"Correo registrado")
        vm.registrar(app){};esperar {vm.errorGeneral.value!=null};assertEquals("Correo registrado",vm.errorGeneral.value)
    }
    @Test fun loginViewModelControlaValidacionBienvenidaYErrorSinGuardarPassword()=runBlocking {
        val vm=modelo(LoginViewModel());var ingreso=false
        vm.login(app){ingreso=true};assertNotNull(vm.emailError.value);assertNotNull(vm.passwordError.value)
        vm.onEmailChange("no-es-correo");assertNotNull(vm.emailError.value)
        vm.onEmailChange(usuario.correo);vm.onPasswordChange("x");assertNotNull(vm.passwordError.value)
        vm.onPasswordChange("Password2026!");vm.login(app){ingreso=true};esperar {ingreso}
        assertTrue(vm.accesoConcedido.value);assertEquals("",vm.password.value);assertFalse(vm.cargando.value)
        coEvery {api.login(any())} throws ErrorBackend(401,"Credenciales incorrectas")
        vm.onPasswordChange("Password2026!");vm.login(app){};esperar {vm.passwordError.value!=null}
        assertEquals("Credenciales incorrectas",vm.passwordError.value);assertFalse(vm.accesoConcedido.value)
    }
    @Test fun mensajesSoloPuedenLeerseYEnviarseEnConversacionPropiaDelPaciente()=runBlocking {
        repo.entrar(usuario.correo,"Password2026!")
        val mensajes=MensajeRepository(app);mensajes.enviar(usuario.correo," Hola ")
        assertEquals("Hola",mensajes.observar(usuario.correo).first().single().texto)
        try{mensajes.enviar("ajeno@example.com","Hola");fail()}catch(_:IllegalArgumentException){}
        try{mensajes.observar("ajeno@example.com").first();fail()}catch(_:IllegalArgumentException){}
        try{mensajes.enviar(usuario.correo," ");fail()}catch(_:IllegalArgumentException){}
        val vm=modelo(MensajesViewModel(app));esperar {vm.usuario!=null};vm.escribir("Mensaje nuevo");vm.enviar()
        esperar {vm.mensajes.size==2};assertEquals("",vm.texto)
    }
    @Test fun administracionBuscaCuentaYGuardaVinculoVerificado()=runBlocking {
        SesionBackend.abrir(usuario.copy(id=1,rol="ADMIN"),"Password2026!")
        coEvery {perfiles.obtener(9,any())} returns PerfilProfesionalRemoto(9,"Profesional",true)
        coEvery {api.cambiarRol(42,any(),any())} returns usuario.copy(rol="ESPECIALISTA",especialistaId=9)
        val vm=modelo(UsuariosAdminViewModel(app));vm.buscar("x");esperar {!vm.ocupado};assertNotNull(vm.mensaje)
        vm.buscar("42");esperar {vm.cuenta!=null};assertEquals(usuario.correo,vm.cuenta!!.correo)
        vm.guardar("ESPECIALISTA","9");esperar {vm.cuenta?.rol=="ESPECIALISTA"};assertTrue(vm.mensaje!!.contains("próximo"))
    }
    @Test fun perfilViewModelObservaCambiosYNoPierdeEstadoTrasError()=runBlocking {
        repo.entrar(usuario.correo,"Password2026!")
        val vm=modelo(PerfilViewModel(app));esperar {vm.perfil!=null}
        vm.actualizarFotoPerfil(null);assertNull(vm.fotoPerfilUri.value)
        vm.guardar("Ana María","12345678","Prueba");esperar {vm.mensaje=="Cambios guardados"}
        vm.guardar("Ana María","no-telefono","Prueba");esperar {vm.mensaje!="Cambios guardados"};assertFalse(vm.guardando)
    }
}
