package com.example.sanaesencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.repository.*
import com.example.sanaesencia.di.AppContainer
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class FlujosServidorViewModelTest {
    private val dispatcher=StandardTestDispatcher()
    private val modelos=mutableListOf<ViewModel>()
    private inline fun <reified T:ViewModel> modelo(vm:T)=vm.also { modelos+=it }
    private lateinit var repo:ReservasRepository
    private lateinit var catalogo:CatalogoRepository
    private lateinit var api:CatalogoApi
    private lateinit var container:AppContainer
    private val servicio=ServicioRemoto(100,"Terapia","Atención",BigDecimal("30000"),60,true,true)
    private val profesional=EspecialistaRemoto(9,"Profesional de prueba","Terapia","Descripción",true,true,true,listOf(servicio))
    private val fecha="2099-01-15"
    private val turno=TurnoRemoto("${fecha}T09:00:00","${fecha}T10:00:00",listOf(BoxDisponible(2,"Box 2")))
    private val cita=ReservaRemota(77,42,"Paciente",9,profesional.nombre,100,servicio.nombre,servicio.precio,
        turno.inicio,turno.fin,Modalidad.PRESENCIAL,"CONFIRMADA",2,"Box 2",null,false)
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repo=mockk();catalogo=mockk();api=mockk();container=mockk()
        every {container.reservasRepository} returns repo
        every {container.catalogoRepository} returns catalogo
        every {container.catalogoApi} returns api
        every {repo.cambios} returns MutableSharedFlow()
        coEvery {catalogo.exigirVigente()} just Runs
        coEvery {catalogo.idServicioServidor(10)} returns 100
        coEvery {catalogo.idEspecialistaServidor(90)} returns 9
        coEvery {catalogo.idEspecialistaLocal(9)} returns 90
        coEvery {api.servicio(100)} returns servicio
        coEvery {api.especialistas(100)} returns listOf(profesional)
        coEvery {api.especialista(9)} returns profesional
        coEvery {repo.diasDisponibles(9,100,any(),any(),any())} returns linkedMapOf(fecha to listOf(turno))
        coEvery {repo.disponibilidad(9,100,fecha,any(),any())} returns listOf(turno)
        coEvery {repo.obtener(77)} returns cita
        coEvery {repo.guardar(any(),any())} returns cita
    }
    @After fun limpiar() { modelos.forEach {it.viewModelScope.cancel()};unmockkAll();Dispatchers.resetMain() }
    private suspend fun TestScope.nueva():ReservaServidorViewModel = modelo(ReservaServidorViewModel(container)).also {
        it.preparar(10,90,null);advanceUntilIdle()
    }
    @Test fun reservaSoloConDiaYHoraValidadosYNoEnviaDuplicados()=runTest {
        val vm=nueva();assertEquals(listOf(fecha),vm.estado.value.diasDisponibles)
        vm.dia("2099-01-16");assertNull(vm.estado.value.dia)
        vm.guardar();coVerify(exactly=0){repo.guardar(any(),any())}
        vm.dia(fecha);advanceUntilIdle()
        vm.hora("${fecha}T12:00:00");assertNull(vm.estado.value.inicio)
        vm.hora(turno.inicio);vm.guardar();advanceUntilIdle()
        assertEquals(77L,vm.estado.value.recibo!!.id);assertFalse(vm.estado.value.ocupada)
        vm.guardar();coVerify(exactly=1){repo.guardar(null,match {it.especialistaId==9L && it.servicioId==100L && it.inicio==turno.inicio})}
    }
    @Test fun reprogramacionConservaProfesionalIdYDiaCuandoSigueDisponible()=runTest {
        val vm=modelo(ReservaServidorViewModel(container));vm.preparar(null,null,77);advanceUntilIdle()
        assertEquals(fecha,vm.estado.value.dia)
        vm.especialista(99);assertEquals(9L,vm.estado.value.especialistaId)
        vm.modalidad(Modalidad.ONLINE);advanceUntilIdle();assertEquals(fecha,vm.estado.value.dia)
        vm.hora(turno.inicio);vm.guardar();advanceUntilIdle()
        coVerify {repo.guardar(77,match {it.modalidad==Modalidad.ONLINE})}
        coVerify {repo.diasDisponibles(9,100,Modalidad.ONLINE,77,any())}
    }
    @Test fun errorDeCalendarioSeDistingueDeAgendaVaciaYPermiteReintento()=runTest {
        coEvery {repo.diasDisponibles(9,100,any(),any(),any())} throws ErrorBackend(503,"Sin conexión")
        val vm=nueva();assertEquals("Sin conexión",vm.estado.value.errorDias);assertFalse(vm.estado.value.cargandoDias)
        coEvery {repo.diasDisponibles(9,100,any(),any(),any())} returns emptyMap()
        vm.cargarDias();advanceUntilIdle();assertNull(vm.estado.value.errorDias);assertTrue(vm.estado.value.diasDisponibles.isEmpty())
    }
    @Test fun cambioRapidoDeModalidadNoPublicaUnCalendarioAtrasado()=runTest {
        val pendiente=CompletableDeferred<Map<String,List<TurnoRemoto>>>()
        coEvery {repo.diasDisponibles(9,100,Modalidad.PRESENCIAL,any(),any())} coAnswers {pendiente.await()}
        val vm=modelo(ReservaServidorViewModel(container));vm.preparar(10,90,null);runCurrent()
        assertTrue(vm.estado.value.cargandoDias)
        vm.modalidad(Modalidad.ONLINE);advanceUntilIdle()
        pendiente.complete(mapOf("2099-02-01" to listOf(turno)));runCurrent()
        assertEquals(Modalidad.ONLINE,vm.estado.value.modalidad);assertEquals(listOf(fecha),vm.estado.value.diasDisponibles)
    }
    @Test fun cupoOcupadoTrasElegirDiaDesapareceDelCalendario()=runTest {
        val vm=nueva();coEvery {repo.disponibilidad(9,100,fecha,any(),any())} returns emptyList()
        vm.dia(fecha);advanceUntilIdle();assertNull(vm.estado.value.dia);assertTrue(vm.estado.value.diasDisponibles.isEmpty())
    }
    @Test fun conflictoReconsultaPeroResultadoInciertoImpideRepetirReserva()=runTest {
        val vm=nueva();vm.dia(fecha);advanceUntilIdle();vm.hora(turno.inicio)
        coEvery {repo.guardar(any(),any())} throws ErrorBackend(409,"Cupo ocupado")
        vm.guardar();advanceUntilIdle();assertFalse(vm.estado.value.ambigua);assertEquals("Cupo ocupado",vm.estado.value.error)
        vm.hora(turno.inicio);coEvery {repo.guardar(any(),any())} throws ErrorBackend(null,"Sin conexión")
        vm.guardar();advanceUntilIdle();assertTrue(vm.estado.value.ambigua)
        vm.guardar();coVerify(exactly=2){repo.guardar(any(),any())}
    }
    @Test fun precioCambiadoExigeRevisarAntesDeConfirmar()=runTest {
        val vm=nueva();vm.dia(fecha);advanceUntilIdle();vm.hora(turno.inicio)
        coEvery {api.servicio(100)} returns servicio.copy(precio=BigDecimal("35000"))
        vm.guardar();advanceUntilIdle();assertTrue(vm.estado.value.error!!.contains("cambió"))
        coVerify(exactly=0){repo.guardar(any(),any())}
    }
    @Test fun errorAlPrepararPermiteReintentarYNoAceptaPerfilQueYaNoOfreceServicio()=runTest {
        coEvery {catalogo.exigirVigente()} throws ErrorBackend(503,"Sin catálogo")
        val vm=nueva();assertTrue(vm.estado.value.errorCarga)
        coEvery {catalogo.exigirVigente()} just Runs
        vm.reintentar();advanceUntilIdle();assertFalse(vm.estado.value.errorCarga)
        coEvery {api.especialistas(100)} returns emptyList()
        vm.preparar(null,null,77);advanceUntilIdle();assertTrue(vm.estado.value.errorCarga)
    }
    @Test fun ubicacionDaRecomendacionSinBloquearReservaSiGpsFalla()=runTest {
        val vm=nueva()
        coEvery {repo.sugerencia(9,100,any(),any())} returns SugerenciaRemota(30.0,true,true,true,"Puedes elegir online")
        vm.ubicacion(-33.0 to -71.0);advanceUntilIdle();assertTrue(vm.estado.value.sugerencia!!.sugiereOnline)
        coEvery {repo.sugerencia(9,100,any(),any())} throws ErrorBackend(503,"GPS")
        vm.ubicacion(-33.0 to -71.0);advanceUntilIdle();assertNotNull(vm.estado.value.errorUbicacion)
        vm.ubicacion(null);assertNull(vm.estado.value.sugerencia)
    }
    @Test fun listaDeCitasRecargaAlCambiarMesYAlCancelarOAtender()=runTest {
        coEvery {repo.listar(any())} returns listOf(cita)
        coEvery {repo.cancelar(77)} just Runs
        coEvery {repo.atender(77)} returns cita.copy(estado="ATENDIDA")
        val vm=modelo(ListaReservasViewModel(repo));advanceUntilIdle();assertEquals(1,vm.estado.value.citas.size)
        val mes=vm.estado.value.periodo;vm.mes(1);advanceUntilIdle();assertEquals(mes.desplazar(1),vm.estado.value.periodo)
        vm.cambiarEstado(77);advanceUntilIdle();assertEquals("Reserva cancelada",vm.estado.value.mensaje)
        vm.cambiarEstado(77,true);advanceUntilIdle();assertEquals("Sesión marcada como atendida",vm.estado.value.mensaje)
        coEvery {repo.listar(any())} throws ErrorBackend(503,"Agenda sin conexión")
        vm.cargar();advanceUntilIdle();assertTrue(vm.estado.value.citas.isEmpty());assertNotNull(vm.estado.value.error)
    }
    @Test fun editarHorariosDistingueCupoPublicadoDeFranjaSemanal()=runTest {
        coEvery {repo.horarios(9)} returns listOf(HorarioRemoto("MONDAY","09:00:00","19:00:00"))
        coEvery {repo.guardarHorarios(9,any())} coAnswers {secondArg()}
        val vm=modelo(HorariosServidorViewModel(repo));vm.cargar(9);advanceUntilIdle();assertTrue(vm.estado.value.cargado)
        vm.editar(0,HorarioRemoto("SATURDAY","09:00","19:00"));vm.guardar();advanceUntilIdle()
        assertTrue(vm.estado.value.mensaje!!.contains("Revisa"));coVerify(exactly=0){repo.guardarHorarios(any(),any())}
        vm.editar(0,HorarioRemoto("SATURDAY","09:30","10:30","2099-01-17",true));vm.guardar();advanceUntilIdle()
        assertEquals("Horarios guardados en el servidor",vm.estado.value.mensaje)
        vm.agregar();assertEquals(2,vm.estado.value.horarios.size);vm.quitar(0);assertEquals(1,vm.estado.value.horarios.size)
        vm.quitar(0);vm.guardar();advanceUntilIdle();assertEquals("Agrega al menos una franja horaria",vm.estado.value.mensaje)
    }
    @Test fun recorridoControlaDobleConsultaErrorYCancelacion()=runTest {
        val rutas=mockk<RutasRepository>();val pendiente=CompletableDeferred<RecorridoSede>()
        coEvery {rutas.consultar(any(),any())} coAnswers {pendiente.await()}
        val vm=modelo(RutasViewModel(rutas));vm.consultar(-33.0,-71.0);runCurrent();vm.consultar(-33.0,-71.0)
        coVerify(exactly=1){rutas.consultar(any(),any())}
        vm.limpiar();pendiente.complete(RecorridoSede(100.0,60.0));runCurrent();assertEquals(RutaUiState.Inicial,vm.estado.value)
        coEvery {rutas.consultar(any(),any())} throws ErrorRuta("Límite de consultas")
        vm.consultar(-33.0,-71.0);advanceUntilIdle();assertEquals(RutaUiState.Error("Límite de consultas"),vm.estado.value)
        coEvery {rutas.consultar(any(),any())} returns RecorridoSede(200.0,120.0)
        vm.consultar(-33.0,-71.0);advanceUntilIdle();assertTrue(vm.estado.value is RutaUiState.Exito)
    }
}
