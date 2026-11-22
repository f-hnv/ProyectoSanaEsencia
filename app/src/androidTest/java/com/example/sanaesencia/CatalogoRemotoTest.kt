package com.example.sanaesencia

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.repository.*
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.utils.precioExacto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.math.BigDecimal

class CatalogoRemotoTest {
    private lateinit var db:AppDatabase
    private lateinit var api:ApiFalsa
    private lateinit var repo:CatalogoRepository
    private var ahora=100_000L
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    class ApiFalsa:CatalogoApi {
        var servicio=ServicioRemoto(1,"Servicio MySQL","Desde servidor",BigDecimal("34567.50"),75,true,true)
        var perfil=EspecialistaRemoto(1,"Profesional MySQL","Especialidad","Descripción",true,true,true,listOf(servicio))
        var servicios:List<ServicioRemoto>?=null
        var perfiles:List<EspecialistaRemoto>?=null
        var fallo=false
        var solicitudes=0
        override suspend fun servicios():List<ServicioRemoto> { solicitudes++; if(fallo) throw java.io.IOException(); return servicios ?: listOf(servicio) }
        override suspend fun especialistas(servicioId:Long?):List<EspecialistaRemoto> { if(fallo) throw java.io.IOException(); return perfiles ?: listOf(perfil) }
        override suspend fun servicio(id:Long):ServicioRemoto { if(fallo) throw java.io.IOException(); return servicio }
        override suspend fun especialista(id:Long):EspecialistaRemoto { if(fallo) throw java.io.IOException(); return perfil }
    }
    @Before fun preparar() {
        db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).addCallback(AppDatabase.INTEGRIDAD).build()
        api=ApiFalsa(); repo=CatalogoRepository(db,api,"prueba") { ahora }
    }
    @After fun cerrar() { db.close() }
    @Test fun idsCoincidentesNoSobrescribenUnaReservaAntigua() = runBlocking {
        db.servicioDao().insertar(ServicioEntity(1,"Servicio antiguo","Anterior","Local",50,1000))
        db.especialistaDao().insertar(EspecialistaEntity(1,"Profesional antiguo","Anterior"))
        val cita=db.citaDao().insertar(CitaEntity(emailPaciente="historia@example.com",servicioId=1,especialistaId=1,
            fechaHoraInicio=1_000_000,duracionMin=50,modalidad=Modalidad.ONLINE))
        repo.actualizar()
        val remoto=repo.estado.value.servicios.single()
        assertNotEquals(1L,remoto.id); assertEquals(1L,repo.idServicioServidor(remoto.id))
        assertEquals("Servicio antiguo",db.citaDao().obtenerDetallePorId(cita)!!.servicio.nombre)
        assertEquals(BigDecimal("34567.50"),remoto.precioExacto())
        assertEquals(75,remoto.duracionMin)
    }
    @Test fun actualizarNoDuplicaIdsNiBorraCitasVinculadas() = runBlocking {
        repo.actualizar()
        val s=repo.estado.value.servicios.single(); val p=repo.estado.value.especialistas.single()
        val cita=db.citaDao().insertar(CitaEntity(emailPaciente="actualizacion@example.com",servicioId=s.id,especialistaId=p.id,
            fechaHoraInicio=1_000_000,duracionMin=75,modalidad=Modalidad.ONLINE))
        api.servicio=api.servicio.copy(precio=BigDecimal("42000.25"),duracionMinutos=90)
        api.perfil=api.perfil.copy(servicios=listOf(api.servicio))
        repo.actualizar(true)
        assertEquals(s.id,repo.estado.value.servicios.single().id); assertEquals(1,db.servicioDao().contar())
        assertEquals(BigDecimal("42000.25"),repo.estado.value.servicios.single().precioExacto())
        assertEquals(75,db.citaDao().obtenerDetallePorId(cita)!!.cita.duracionMin)
    }
    @Test fun conexionCaidaConservaCopiaPeroNoLaConsideraVigente() = runBlocking {
        repo.actualizar(); api.fallo=true; repo.actualizar(true)
        val estado=repo.estado.value
        assertEquals(1,estado.servicios.size); assertFalse(estado.vigente); assertFalse(estado.cargando); assertNotNull(estado.error)
        api.fallo=false; repo.actualizar(true); assertTrue(repo.estado.value.vigente); assertNull(repo.estado.value.error)
    }
    @Test fun cargaParcialFallidaNoPublicaDatosMezclados() = runBlocking {
        repo.actualizar(); val anterior=repo.estado.value
        api.servicio=api.servicio.copy(nombre="No debe publicarse")
        api.perfil=api.perfil.copy(nombre="")
        repo.actualizar(true)
        assertEquals(anterior.servicios,repo.estado.value.servicios)
        assertFalse(repo.estado.value.vigente)
        assertEquals("Servicio MySQL",db.servicioDao().obtenerPorId(anterior.servicios.single().id)!!.nombre)
    }
    @Test fun desactivadosDesaparecenPeroSusCitasSeConservan() = runBlocking {
        repo.actualizar(); val s=repo.estado.value.servicios.single(); val p=repo.estado.value.especialistas.single()
        val cita=db.citaDao().insertar(CitaEntity(emailPaciente="inactiva@example.com",servicioId=s.id,especialistaId=p.id,
            fechaHoraInicio=1_000_000,duracionMin=75,modalidad=Modalidad.ONLINE))
        api.servicios=emptyList(); api.perfiles=emptyList(); repo.actualizar(true)
        assertTrue(repo.estado.value.vigente); assertTrue(repo.estado.value.servicios.isEmpty())
        assertNotNull(db.citaDao().obtenerDetallePorId(cita))
        assertFalse(db.servicioDao().obtenerPorId(s.id)!!.activo)
    }
    @Test fun filtroDeEspecialistasExigeAsignacionExplicita() = runBlocking {
        api.perfiles=listOf(api.perfil,api.perfil.copy(id=2,nombre="Sin asignación",servicios=emptyList()))
        repo.actualizar()
        val s=repo.estado.value.servicios.single()
        val especialistas=EspecialistaRepository(db.especialistaDao()) { repo }.observarParaServicio(s.id).first()
        assertEquals(listOf("Profesional MySQL"),especialistas.map { it.nombre })
    }
    @Test fun confirmarRechazaPrecioModalidadOAsignacionCambiados() = runBlocking {
        repo.actualizar(); val s=repo.estado.value.servicios.single().id; val p=repo.estado.value.especialistas.single().id
        repo.validarSeleccion(s,p,Modalidad.PRESENCIAL)
        api.servicio=api.servicio.copy(precio=BigDecimal("40000"))
        try { repo.validarSeleccion(s,p,Modalidad.PRESENCIAL); fail() } catch(e:ErrorBackend) { assertEquals(409,e.estado) }
        api.perfil=api.perfil.copy(servicios=emptyList())
        try { repo.validarSeleccion(s,p,Modalidad.PRESENCIAL); fail() } catch(e:ErrorBackend) { assertEquals(409,e.estado) }
        api.perfil=api.perfil.copy(servicios=listOf(api.servicio),permiteOnline=false)
        try { repo.validarSeleccion(s,p,Modalidad.ONLINE); fail() } catch(e:ErrorBackend) { assertEquals(409,e.estado) }
    }
    @Test fun cacheSeRecuperaEnUnaNuevaInstanciaYSinDatosDeOtroOrigen() = runBlocking {
        repo.actualizar()
        api.fallo=true
        val reinicio=CatalogoRepository(db,api,"prueba"); reinicio.actualizar()
        assertEquals(1,reinicio.estado.value.servicios.size); assertFalse(reinicio.estado.value.vigente)
        val otro=CatalogoRepository(db,api,"otro-servidor"); otro.actualizar()
        assertTrue(otro.estado.value.servicios.isEmpty())
    }
    @Test fun consultasSimultaneasCompartenUnaActualizacion() = runBlocking {
        coroutineScope { (1..4).map { async { repo.actualizar() } }.awaitAll() }
        assertEquals(1,api.solicitudes)
        ahora+=31_000; repo.actualizar(); assertEquals(2,api.solicitudes)
    }
    @Test fun respuestaVaciaEsDistintaDeUnaConexionFallida() = runBlocking {
        api.servicios=emptyList(); api.perfiles=emptyList(); repo.actualizar()
        assertTrue(repo.estado.value.vigente); assertTrue(repo.estado.value.servicios.isEmpty()); assertNull(repo.estado.value.error)
    }
}
