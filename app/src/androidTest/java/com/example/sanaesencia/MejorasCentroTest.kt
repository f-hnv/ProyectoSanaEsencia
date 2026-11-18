package com.example.sanaesencia

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.repository.*
import com.example.sanaesencia.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class MejorasCentroTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun fecha(dia:Int,hora:Int)=Calendar.getInstance(HorariosAgenda.zona).apply{clear();set(2026,Calendar.NOVEMBER,dia,hora,0)}.timeInMillis
    private suspend fun base():AppDatabase {
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).addCallback(AppDatabase.INTEGRIDAD).build()
        db.servicioDao().insertar(ServicioEntity(id=1,nombre="Sesión",categoria="Prueba",descripcion="",duracionMin=50,precioClp=1,permiteOnline=true))
        db.especialistaDao().insertarTodos((1L..4L).map{EspecialistaEntity(it,"Profesional $it","Prueba")})
        return db
    }
    @Test fun pacienteNoPuedeDuplicarAunqueCambieDeProfesional()=runBlocking {
        val db=base()
        try {
            val repo=CitaRepository(db){fecha(1,8)};val hora=fecha(2,10)
            val primera=repo.agendar("paciente@test.cl",1,1,hora,Modalidad.PRESENCIAL) as Resultado.Exito
            assertEquals(ErrorAgenda.PacienteOcupado,(repo.agendar("paciente@test.cl",1,2,hora,Modalidad.PRESENCIAL) as Resultado.Error).error)
            assertFalse(hora in repo.observarHorariosDisponibles(hora,2,50,emailPaciente="paciente@test.cl").first())
            assertTrue(hora in repo.observarHorariosDisponibles(hora,2,50,emailPaciente="otra@test.cl").first())
            assertEquals(ErrorAgenda.HorarioOcupado,(repo.agendar("otra@test.cl",1,1,hora,Modalidad.PRESENCIAL) as Resultado.Error).error)
            assertTrue(repo.cancelar(primera.datos) is Resultado.Exito)
            assertTrue(repo.agendar("paciente@test.cl",1,2,hora,Modalidad.PRESENCIAL) is Resultado.Exito)
            assertEquals(ErrorAgenda.HorarioFueraAtencion,(repo.agendar("libre@test.cl",1,3,fecha(2,13),Modalidad.PRESENCIAL) as Resultado.Error).error)
        } finally{db.close()}
    }
    @Test fun boxesSeOcupanSeLiberanYOnlineNoNecesitaBox()=runBlocking {
        val db=base()
        try {
            val repo=CitaRepository(db){fecha(1,8)};val hora=fecha(2,10)
            val ids=(1L..3L).map{(repo.agendar("p$it@test.cl",1,it,hora,Modalidad.PRESENCIAL) as Resultado.Exito).datos}
            val boxes=ids.map{db.citaDao().obtenerDetallePorId(it)!!.cita.boxId}
            assertEquals(3,boxes.distinct().size);assertFalse(boxes.contains(null))
            assertEquals(ErrorAgenda.SinBox,(repo.agendar("cuarto@test.cl",1,4,hora,Modalidad.PRESENCIAL) as Resultado.Error).error)
            assertTrue(repo.agendar("online@test.cl",1,4,hora,Modalidad.ONLINE) is Resultado.Exito)
            assertTrue(repo.cancelar(ids.first()) is Resultado.Exito)
            assertTrue(repo.agendar("nuevo@test.cl",1,1,hora,Modalidad.PRESENCIAL) is Resultado.Exito)
        } finally{db.close()}
    }
    @Test fun dosSolicitudesConcurrentesSoloConfirmanUna()=runBlocking {
        val db=base()
        try {
            val repo=CitaRepository(db){fecha(1,8)}
            val resultados=coroutineScope{(1..2).map{async(Dispatchers.IO){repo.agendar("p$it@test.cl",1,1,fecha(2,10),Modalidad.PRESENCIAL)}}.awaitAll()}
            assertEquals(1,resultados.count{it is Resultado.Exito})
            assertEquals(1,resultados.count{it is Resultado.Error})
        } finally{db.close()}
    }
    @Test fun baseRechazaTraslapeInclusoFueraDelRepositorio()=runBlocking {
        val db=base()
        try {
            db.citaDao().insertar(CitaEntity(emailPaciente="uno@test.cl",servicioId=1,especialistaId=1,fechaHoraInicio=fecha(2,10),duracionMin=50,modalidad=Modalidad.ONLINE))
            var rechazado=false
            try {db.citaDao().insertar(CitaEntity(emailPaciente="dos@test.cl",servicioId=1,especialistaId=1,fechaHoraInicio=fecha(2,10),duracionMin=50,modalidad=Modalidad.ONLINE))}
            catch(_:android.database.sqlite.SQLiteConstraintException){rechazado=true}
            assertTrue(rechazado)
        } finally{db.close()}
    }
    @Test fun perfilRolesContrasenaYConversacionPersisten()=runBlocking {
        val cuenta=CuentaRepository(context);val email="perfil.${System.nanoTime()}@gmail.com"
        cuenta.registrar("Paciente de prueba",email,"Prueba123")
        assertNotNull(cuenta.entrar(email,"Prueba123"))
        cuenta.editarPerfil("Nombre actualizado","+56 9 1234 5678")
        val msg=MensajeRepository(context)
        msg.enviar(email,"Consulta de prueba local")
        assertEquals("Consulta de prueba local",msg.observar(email).first().single().texto)
        cuenta.cambiarClave("Prueba123","Nueva123","Nueva123")
        cuenta.salir(); assertNull(cuenta.actual())
        assertNull(cuenta.entrar(email,"Prueba123"))
        assertNotNull(cuenta.entrar(email,"Nueva123"))
        assertEquals("+56 9 1234 5678",cuenta.actual()!!.telefono)
        assertEquals("Nombre actualizado",cuenta.actual()!!.nombre)
        var bloqueado=false
        try{msg.enviar("ajeno@gmail.com","No autorizado")}catch(_:IllegalArgumentException){bloqueado=true}
        assertTrue(bloqueado)
        assertNotEquals("Nueva123",cuenta.actual()!!.hash)
        assertNotNull(cuenta.entrar(CuentaRepository.ADMIN_DEMO,CuentaRepository.CLAVE_DEMO))
        msg.enviar(email,"Respuesta del centro de demostración")
        assertEquals(2,msg.observar(email).first().size)
        val profesional="profesional.${System.nanoTime()}@gmail.com"
        cuenta.crearEspecialista("Profesional Prueba",profesional,"Prueba123","Psicología","Datos ficticios",listOf(1))
        assertEquals("ESPECIALISTA",cuenta.entrar(profesional,"Prueba123")!!.rol)
        cuenta.salir()
    }
}
