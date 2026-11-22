package com.example.sanaesencia

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.repository.*
import com.example.sanaesencia.domain.model.*
import com.example.sanaesencia.utils.FechasReservas
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
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE,application=Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PersistenciaYAgendaTest {
    private lateinit var db:AppDatabase
    private val dispatcher=UnconfinedTestDispatcher()
    private val modelos=mutableListOf<ViewModel>()
    private val inicio=FechasReservas.instante("2030-01-07T09:00:00")
    private lateinit var citas:CitaRepository
    private fun <T:ViewModel> modelo(vm:T)=vm.also {modelos+=it}
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        db=Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(),AppDatabase::class.java).allowMainThreadQueries().build()
        citas=CitaRepository(db){inicio-3*86_400_000L}
    }
    @After fun limpiar(){modelos.forEach {it.viewModelScope.cancel()};db.close();unmockkAll();Dispatchers.resetMain()}
    private suspend fun centro() {
        db.servicioDao().insertar(ServicioEntity(1,"Terapia","Sesión","Salud",60,30000,true))
        db.servicioDao().insertar(ServicioEntity(2,"Consulta","Sesión","Salud",60,20000,false))
        db.especialistaDao().insertar(EspecialistaEntity(1,"Profesional 1","Terapia"))
        db.especialistaDao().insertar(EspecialistaEntity(2,"Profesional 2","Terapia"))
        db.openHelper.writableDatabase.execSQL("INSERT INTO boxes(id,nombre,activo) VALUES(1,'Box de prueba',1)")
    }
    private fun error(resultado:Resultado<*>,esperado:ErrorAgenda){assertEquals(esperado,(resultado as Resultado.Error).error)}
    private suspend fun esperar(condicion:()->Boolean) {repeat(300){dispatcher.scheduler.advanceUntilIdle();if(condicion())return;delay(10)};assertTrue("No se alcanzó el estado esperado",condicion())}
    @Test fun reservaLocalEsAtomicaYEvitaCrucesDeProfesionalPacienteYBox()=runBlocking {
        centro();val id=(citas.agendar("a@x.cl",1,1,inicio,Modalidad.PRESENCIAL) as Resultado.Exito).datos
        assertEquals(1L,citas.obtenerDetallePorId(id)!!.cita.boxId)
        error(citas.agendar("b@x.cl",1,1,inicio,Modalidad.PRESENCIAL),ErrorAgenda.HorarioOcupado)
        error(citas.agendar("a@x.cl",1,2,inicio,Modalidad.PRESENCIAL),ErrorAgenda.PacienteOcupado)
        error(citas.agendar("b@x.cl",1,2,inicio,Modalidad.PRESENCIAL),ErrorAgenda.SinBox)
        val online=(citas.agendar("b@x.cl",1,2,inicio,Modalidad.ONLINE) as Resultado.Exito).datos
        assertNull(citas.obtenerDetallePorId(online)!!.cita.boxId)
        assertEquals(1,citas.observarPorPaciente("a@x.cl").first().size)
        assertEquals(1,citas.observarProximas("a@x.cl").first().size)
    }
    @Test fun erroresDeFechaServicioModalidadYProfesionalNoEscribenCitas()=runBlocking {
        centro()
        error(citas.agendar("a@x.cl",1,1,inicio-4*86_400_000L,Modalidad.ONLINE),ErrorAgenda.FechaPasada)
        error(citas.agendar("a@x.cl",99,1,inicio,Modalidad.ONLINE),ErrorAgenda.ServicioNoDisponible)
        error(citas.agendar("a@x.cl",1,99,inicio,Modalidad.ONLINE),ErrorAgenda.EspecialistaNoDisponible)
        error(citas.agendar("a@x.cl",2,1,inicio,Modalidad.ONLINE),ErrorAgenda.ModalidadNoDisponible)
        error(citas.agendar("a@x.cl",1,1,inicio+4*3_600_000L,Modalidad.ONLINE),ErrorAgenda.HorarioFueraAtencion)
        val cercana=CitaRepository(db){inicio-6*3_600_000L}
        error(cercana.agendar("a@x.cl",1,1,inicio,Modalidad.ONLINE),ErrorAgenda.AnticipacionInsuficiente)
        assertTrue(citas.observarPorPaciente("a@x.cl").first().isEmpty())
    }
    @Test fun cancelacionLiberaCupoYRechazaEstadoInvalidoOFueraDePlazo()=runBlocking {
        centro();val id=(citas.agendar("a@x.cl",1,1,inicio,Modalidad.PRESENCIAL) as Resultado.Exito).datos
        val cercana=CitaRepository(db){inicio-6*3_600_000L}
        error(cercana.cancelar(id),ErrorAgenda.CambioFueraPlazo)
        assertTrue(citas.cancelar(id) is Resultado.Exito)
        assertEquals(EstadoCita.CANCELADA,citas.obtenerDetallePorId(id)!!.cita.estado)
        error(citas.cancelar(id),ErrorAgenda.EstadoInvalido);error(citas.cancelar(999),ErrorAgenda.CitaNoEncontrada)
        assertTrue(citas.agendar("a@x.cl",1,1,inicio,Modalidad.PRESENCIAL) is Resultado.Exito)
    }
    @Test fun reprogramacionConservaRegistroYOnlineLiberaBox()=runBlocking {
        centro();val id=(citas.agendar("a@x.cl",1,1,inicio,Modalidad.PRESENCIAL) as Resultado.Exito).datos
        assertTrue(citas.reprogramar(id,inicio+3_600_000L,Modalidad.ONLINE) is Resultado.Exito)
        val r=citas.obtenerDetallePorId(id)!!.cita
        assertEquals(id,r.id);assertEquals(Modalidad.ONLINE,r.modalidad);assertNull(r.boxId)
        error(citas.reprogramar(id,inicio-4*86_400_000L),ErrorAgenda.FechaPasada)
        error(citas.reprogramar(id,inicio+4*3_600_000L),ErrorAgenda.HorarioFueraAtencion)
        error(citas.reprogramar(999,inicio),ErrorAgenda.CitaNoEncontrada)
        citas.cancelar(id);error(citas.reprogramar(id,inicio),ErrorAgenda.EstadoInvalido)
    }
    @Test fun disponibilidadExcluyeCitaPropiaAlEditarYRespetaModalidad()=runBlocking {
        centro();val id=(citas.agendar("a@x.cl",1,1,inicio,Modalidad.PRESENCIAL) as Resultado.Exito).datos
        assertFalse(inicio in citas.observarHorariosDisponibles(inicio,1,60,null,"a@x.cl").first())
        assertTrue(inicio in citas.observarHorariosDisponibles(inicio,1,60,id,"a@x.cl").first())
        assertFalse(inicio in citas.observarHorariosDisponibles(inicio,2,60,null,"b@x.cl").first())
        assertTrue(inicio in citas.observarHorariosDisponibles(inicio,2,60,null,"b@x.cl",Modalidad.ONLINE).first())
    }
    @Test fun formularioHistoricoAgendaReprogramaYCancelaSinDuplicar()=runBlocking {
        centro();val vm=modelo(AgendaViewModel(citas,ServicioRepository(db.servicioDao()),EspecialistaRepository(db.especialistaDao()),"a@x.cl"))
        esperar {!vm.uiState.value.cargandoEspecialistas}
        vm.onServicioSeleccionado(1,1);esperar {vm.uiState.value.especialistaId==1L}
        vm.onDiaSeleccionado(inicio);esperar {inicio in vm.uiState.value.horariosDisponibles}
        vm.onFechaHoraSeleccionada(inicio);vm.confirmarAgendamiento();esperar {vm.uiState.value.operacion is EstadoOperacion.Exito}
        val id=(vm.uiState.value.operacion as EstadoOperacion.Exito).citaId!!
        vm.resultadoConsumido();assertEquals(EstadoOperacion.Inactivo,vm.uiState.value.operacion)
        vm.prepararEdicion(id);esperar {vm.uiState.value.citaEditandoId==id}
        vm.onModalidadSeleccionada(Modalidad.ONLINE);esperar {!vm.uiState.value.cargandoHorarios}
        vm.onFechaHoraSeleccionada(inicio+3_600_000L);vm.confirmarAgendamiento();esperar {vm.uiState.value.operacion is EstadoOperacion.Exito}
        assertEquals(1,citas.observarPorPaciente("a@x.cl").first().size)
        assertEquals(inicio+3_600_000L,citas.obtenerDetallePorId(id)!!.cita.fechaHoraInicio)
        vm.cancelarCita(id);esperar {vm.uiState.value.operacion is EstadoOperacion.Exito}
        assertEquals(EstadoCita.CANCELADA,citas.obtenerDetallePorId(id)!!.cita.estado)
    }
    @Test fun formularioHistoricoExplicaSeleccionInvalidaYSusCampos()=runBlocking {
        centro();val vm=modelo(AgendaViewModel(citas,ServicioRepository(db.servicioDao()),EspecialistaRepository(db.especialistaDao()),"a@x.cl"))
        esperar {!vm.uiState.value.cargandoEspecialistas};vm.confirmarAgendamiento();assertNotNull(vm.uiState.value.errores.servicio)
        vm.onServicioSeleccionado(999);esperar {vm.uiState.value.errorEspecialistas!=null};assertNull(vm.uiState.value.servicioSeleccionado)
        vm.onServicioSeleccionado(2,1);esperar {vm.uiState.value.servicioSeleccionado?.id==2L}
        vm.onModalidadSeleccionada(Modalidad.ONLINE);assertEquals(Modalidad.PRESENCIAL,vm.uiState.value.modalidad)
    }
    @Test fun repositoriosLocalesFiltranCategoriasYEspecialistas()=runBlocking {
        centro();val servicios=ServicioRepository(db.servicioDao());val perfiles=EspecialistaRepository(db.especialistaDao())
        assertEquals(2,servicios.observarServicios().first().size);assertEquals(listOf("Salud"),servicios.observarCategorias().first())
        assertEquals(2,servicios.observarPorCategoria("Salud").first().size);assertEquals("Terapia",servicios.obtenerPorId(1)!!.nombre)
        assertEquals(2,perfiles.observarEspecialistas().first().size);assertEquals(1L,perfiles.obtenerPorId(1)!!.id)
    }
    @Test fun catalogoSincronizaIdsPrecioYAsignacionesSinConfundirIdsLocales()=runBlocking {
        val api=mockk<CatalogoApi>();val s=ServicioRemoto(100,"Terapia","Descripción",BigDecimal("30000.50"),60,true,true)
        val p=EspecialistaRemoto(9,"Profesional","Terapia","Descripción",true,true,true,listOf(s))
        coEvery {api.servicios()} returns listOf(s);coEvery {api.especialistas(any())} returns listOf(p)
        coEvery {api.servicio(100)} returns s;coEvery {api.especialista(9)} returns p
        val repo=CatalogoRepository(db,api,"https://prueba.example/"){1_000L}
        repo.actualizar();assertTrue(repo.estado.value.vigente)
        val local=repo.estado.value.servicios.single().id;val perfil=repo.estado.value.especialistas.single().id
        assertNotEquals(100L,local);assertEquals(100L,repo.idServicioServidor(local));assertEquals(9L,repo.idEspecialistaServidor(perfil))
        assertEquals(perfil,repo.idEspecialistaLocal(9));repo.validarSeleccion(local,perfil,Modalidad.PRESENCIAL)
        val servicios=ServicioRepository(db.servicioDao()){repo};val perfiles=EspecialistaRepository(db.especialistaDao()){repo}
        assertEquals(local,servicios.observarServicios().first().single().id)
        assertEquals(perfil,perfiles.observarParaServicio(local).first().single().id)
        assertNotNull(servicios.obtenerPorId(local));assertNotNull(perfiles.obtenerPorId(perfil))
        coEvery {api.servicio(100)} returns s.copy(precio=BigDecimal("35000"))
        try{repo.validarSeleccion(local,perfil,Modalidad.PRESENCIAL);fail()}catch(e:ErrorBackend){assertEquals(409,e.estado)}
        val vm=modelo(CatalogoViewModel(repo));esperar {!vm.uiState.value.cargando};assertEquals(1,vm.uiState.value.servicios.size)
    }
    @Test fun falloOJsonInvalidoNoBorraElCatalogoAnteriorNiLoMarcaVigente()=runBlocking {
        val api=mockk<CatalogoApi>();val s=ServicioRemoto(100,"Terapia","Descripción",BigDecimal("30000"),60,true,true)
        coEvery {api.servicios()} returns listOf(s);coEvery {api.especialistas(any())} returns emptyList()
        val repo=CatalogoRepository(db,api,"https://prueba.example/")
        repo.actualizar();val local=repo.estado.value.servicios.single().id
        coEvery {api.servicios()} throws ErrorBackend(503,"Sin conexión")
        repo.actualizar(true);assertFalse(repo.estado.value.vigente);assertEquals(local,repo.estado.value.servicios.single().id)
        assertEquals("Sin conexión",repo.estado.value.error)
        try{repo.exigirVigente();fail()}catch(_:ErrorBackend){}
        coEvery {api.servicios()} returns listOf(s.copy(precio=BigDecimal("-1")))
        repo.actualizar(true);assertFalse(repo.estado.value.vigente);assertEquals(local,repo.estado.value.servicios.single().id)
    }
}
