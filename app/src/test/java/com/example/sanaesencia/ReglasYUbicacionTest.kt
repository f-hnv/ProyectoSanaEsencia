package com.example.sanaesencia

import android.app.Application
import com.example.sanaesencia.data.local.dao.CitaDao
import com.example.sanaesencia.data.local.entity.ServicioEntity
import com.example.sanaesencia.data.local.relation.ResumenPaciente
import com.example.sanaesencia.data.repository.AlertasRepository
import com.example.sanaesencia.domain.analytics.CalculadorRiesgo
import com.example.sanaesencia.domain.model.*
import com.example.sanaesencia.nativo.*
import com.example.sanaesencia.utils.*
import com.example.sanaesencia.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import androidx.lifecycle.viewModelScope

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE,application=Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ReglasYUbicacionTest {
    private val dispatcher=UnconfinedTestDispatcher()
    private val modelos=mutableListOf<androidx.lifecycle.ViewModel>()
    private val trabajos=mutableListOf<Job>()
    @Before fun preparar(){Dispatchers.setMain(dispatcher)}
    @After fun limpiar(){trabajos.forEach {it.cancel()};modelos.forEach {it.viewModelScope.cancel()};unmockkAll();Dispatchers.resetMain()}
    private fun <T:androidx.lifecycle.ViewModel> modelo(vm:T)=vm.also {modelos+=it}
    private suspend fun esperar(condicion:()->Boolean){repeat(200){dispatcher.scheduler.advanceUntilIdle();if(condicion())return;delay(10)};assertTrue(condicion())}
    @Test fun riesgoNoPenalizaSinHistorialYSusUmbralesConsideranCompromisoFuturo() {
        val ahora=100*86_400_000L
        val base=ResumenPaciente("p@example.com",0,0,0,0,null,null)
        assertNull(CalculadorRiesgo.evaluar(base,ahora))
        val bajo=CalculadorRiesgo.evaluar(base.copy(completadas=1,ultimaAtencion=ahora),ahora)!!
        assertEquals(NivelRiesgo.BAJO,bajo.nivel);assertEquals(0,bajo.puntaje);assertTrue(bajo.motivos.isEmpty())
        val alto=CalculadorRiesgo.evaluar(base.copy(canceladas=2,inasistencias=2,primeraCita=0),ahora)!!
        assertEquals(NivelRiesgo.ALTO,alto.nivel);assertEquals(85,alto.puntaje);assertEquals(3,alto.motivos.size)
        assertEquals(100,alto.diasSinAtencion);assertEquals(2,alto.canceladas);assertEquals(2,alto.inasistencias)
        val futuro=CalculadorRiesgo.evaluar(base.copy(completadas=1,futuras=1,primeraCita=0),ahora)!!
        assertEquals(0,futuro.puntaje);assertEquals(1,futuro.citasFuturas)
        for((dias,puntos) in listOf(29 to 0,30 to 10,45 to 20,60 to 30)) {
            val r=CalculadorRiesgo.evaluar(base.copy(completadas=1,ultimaAtencion=ahora-dias*86_400_000L),ahora)!!
            assertEquals(puntos,r.puntaje)
        }
        assertEquals(NivelRiesgo.MEDIO,CalculadorRiesgo.evaluar(base.copy(canceladas=2,ultimaAtencion=ahora),ahora)!!.nivel)
        assertEquals(0,CalculadorRiesgo.evaluar(base.copy(canceladas=1,ultimaAtencion=ahora),ahora)!!.puntaje)
        assertEquals(0,CalculadorRiesgo.evaluar(base.copy(completadas=1,ultimaAtencion=ahora+1),ahora)!!.diasSinAtencion)
    }
    @Test fun alertasSeOrdenanYElFiltroNoModificaLosContadoresTotales()=runBlocking {
        val dao=mockk<CitaDao>();val ahora=100*86_400_000L
        val resumenes=MutableStateFlow(listOf(ResumenPaciente("alto@x.cl",0,2,2,0,null,0),ResumenPaciente("bajo@x.cl",2,0,0,1,ahora,0)))
        every {dao.observarResumenPacientes(any())} returns resumenes
        val repo=AlertasRepository(dao){ahora}
        assertEquals("alto@x.cl",repo.observarAlertas().first().first().emailPaciente)
        val vm=modelo(AlertasViewModel(repo));trabajos+=CoroutineScope(Dispatchers.Main).launch {vm.uiState.collect()}
        esperar {!vm.uiState.value.cargando};assertEquals(2,vm.uiState.value.totalPacientes)
        vm.filtrarPorNivel(NivelRiesgo.ALTO);esperar {vm.uiState.value.alertas.size==1}
        assertEquals(1,vm.uiState.value.conteoPorNivel[NivelRiesgo.BAJO]);assertEquals(2,vm.uiState.value.totalPacientes)
        vm.filtrarPorNivel(null);esperar {vm.uiState.value.alertas.size==2}
    }
    @Test fun validacionesDeFormularioNoPermitenFechasPasadasNiDatosMalformados() {
        val vacio=ValidacionesAgenda.validar(null,null,null,100)
        assertTrue(vacio.hayErrores);assertNotNull(vacio.servicio);assertNotNull(vacio.especialista);assertNotNull(vacio.fechaHora)
        assertTrue(ValidacionesAgenda.validar(1,2,100,100).hayErrores)
        assertFalse(ValidacionesAgenda.validar(1,2,101,100).hayErrores)
        assertNotNull(ValidacionCuenta.correo(""));assertNotNull(ValidacionCuenta.correo("sin-arroba"));assertNull(ValidacionCuenta.correo("a@example.com"))
        assertNotNull(ValidacionCuenta.clave(""));assertNotNull(ValidacionCuenta.clave("123"));assertNull(ValidacionCuenta.clave("123456"))
        assertNull(ValidacionCuenta.telefono(""));assertNull(ValidacionCuenta.telefono("+56 9 1234 5678"));assertNotNull(ValidacionCuenta.telefono("123abc"))
        assertNotNull(ValidacionRegistro.clave("🙂".repeat(20)));assertNotNull(ValidacionRegistro.nombre(""));assertNotNull(ValidacionRegistro.run("00-0"))
    }
    @Test fun preciosConservanDecimalesDelServidorYUsanRespaldoSoloSiFaltan() {
        val s=ServicioEntity(1,"Terapia","Atención","Salud",60,30000,precioServidor="30000.50")
        assertEquals("30000.50",s.precioExacto().toPlainString());assertTrue(s.precioFormateado().contains("50"))
        assertEquals("30000",s.copy(precioServidor=null).precioExacto().toPlainString())
        assertEquals("30000",s.copy(precioServidor="incorrecto").precioExacto().toPlainString())
        assertFalse(s.copy(precioServidor="30000").precioFormateado().contains(",00"))
    }
    @Test fun geografiaYUmbralPersistidoMantienenLasUnidadesCorrectas() {
        assertEquals(0.0,Geo.distanciaMetros(-31.9,-71.5,-31.9,-71.5),0.001)
        val distancia=Geo.distanciaMetros(-33.45,-70.66,-31.9,-71.5)
        assertTrue(distancia in 100_000.0..500_000.0);assertEquals("850 m",Geo.formatearDistancia(850.0));assertTrue(Geo.formatearDistancia(12000.0).contains("km"))
        val app=RuntimeEnvironment.getApplication();ConfiguracionCentro.configurar(app,30f);ConfiguracionCentro.cargar(app)
        assertEquals(30f,ConfiguracionCentro.umbralKm.value)
        try{ConfiguracionCentro.configurar(app,0f);fail()}catch(_:IllegalArgumentException){}
        assertFalse(RecomendacionModalidad.sugerirOnline(Double.NaN,20f,true))
        assertFalse(RecomendacionModalidad.sugerirOnline(20000.0,20f,true));assertTrue(RecomendacionModalidad.sugerirOnline(20001.0,20f,true))
        ConfiguracionCentro.configurar(app,20f)
    }
    @Test fun permisosYErroresGpsLimpianDistanciaAnteriorYPermitenReintentar()=runBlocking {
        val proveedor=mockk<ProveedorUbicacion>()
        every {proveedor.permisosRequeridos} returns arrayOf("ubicacion")
        every {proveedor.tienePermiso()} returns true
        coEvery {proveedor.obtenerUbicacionActual()} returns UbicacionResultado.Exito(-31.9,-71.5,8f)
        val vm=modelo(SedeViewModel(proveedor));assertTrue(vm.tienePermiso());assertEquals("ubicacion",vm.permisosRequeridos.single())
        vm.onPermisoResultado(true);esperar {vm.uiState.value.estado is EstadoUbicacion.Exito};assertNotNull(vm.uiState.value.distanciaTexto)
        vm.onPermisoResultado(false);assertEquals(EstadoUbicacion.PermisoDenegado,vm.uiState.value.estado);assertNull(ConfiguracionCentro.distanciaMetros.value)
        for((respuesta,estado) in listOf(UbicacionResultado.SinPermiso to EstadoUbicacion.PermisoDenegado,
            UbicacionResultado.UbicacionDesactivada to EstadoUbicacion.UbicacionDesactivada,UbicacionResultado.NoDisponible to EstadoUbicacion.NoDisponible)) {
            coEvery {proveedor.obtenerUbicacionActual()} returns respuesta
            vm.actualizarUbicacion();esperar {vm.uiState.value.estado==estado};assertNull(vm.uiState.value.distanciaMetros)
        }
    }
}
