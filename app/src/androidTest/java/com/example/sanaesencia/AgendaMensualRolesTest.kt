package com.example.sanaesencia

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.local.seed.SeedData
import com.example.sanaesencia.data.repository.*
import com.example.sanaesencia.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class AgendaMensualRolesTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun agendaFiltraElMesYElProfesionalSinMezclarPacientes() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).addCallback(AppDatabase.INTEGRIDAD).build()
        try {
            db.servicioDao().insertar(ServicioEntity(1, "Sesión", "", "Prueba", 50, 1000))
            db.especialistaDao().insertarTodos(listOf(EspecialistaEntity(1, "Uno", "Prueba"), EspecialistaEntity(2, "Dos", "Prueba")))
            val mes = PeriodoAgenda(2026, 12)
            fun cita(email: String, profesional: Long, fecha: Long) = CitaEntity(emailPaciente = email,
                servicioId = 1, especialistaId = profesional, fechaHoraInicio = fecha,
                duracionMin = 50, modalidad = Modalidad.ONLINE)
            val propia = db.citaDao().insertar(cita("uno@test.cl", 1, mes.inicio))
            db.citaDao().insertar(cita("dos@test.cl", 2, mes.inicio))
            db.citaDao().insertar(cita("tres@test.cl", 1, mes.fin))
            val diciembre = db.citaDao().observarMesEspecialista(1, mes.inicio, mes.fin).first()
            assertEquals(listOf(propia), diciembre.map { it.cita.id })
            val enero = mes.desplazar(1)
            assertEquals("tres@test.cl", db.citaDao().observarMesEspecialista(1, enero.inicio, enero.fin).first().single().cita.emailPaciente)
        } finally { db.close() }
    }

    @Test fun cambiarModalidadLiberaBoxYVolverAPresencialAsignaUnoLibre() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).addCallback(AppDatabase.INTEGRIDAD).build()
        try {
            db.servicioDao().insertar(ServicioEntity(1, "Sesión", "", "Prueba", 50, 1000, true))
            db.especialistaDao().insertar(EspecialistaEntity(1, "Uno", "Prueba"))
            val ahora = HorariosAgenda.calendario(PeriodoAgenda(2026, 12).inicio).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 8) }.timeInMillis
            val hora = HorariosAgenda.calendario(ahora).apply { set(Calendar.DAY_OF_MONTH, 2); set(Calendar.HOUR_OF_DAY, 10) }.timeInMillis
            val repo = CitaRepository(db) { ahora }
            val id = (repo.agendar("qa@test.cl", 1, 1, hora, Modalidad.PRESENCIAL) as Resultado.Exito).datos
            assertNotNull(repo.obtenerDetallePorId(id)!!.cita.boxId)
            assertTrue(repo.reprogramar(id, hora, Modalidad.ONLINE) is Resultado.Exito)
            assertNull(repo.obtenerDetallePorId(id)!!.cita.boxId)
            assertEquals(Modalidad.ONLINE, repo.obtenerDetallePorId(id)!!.cita.modalidad)
            assertTrue(repo.reprogramar(id, hora, Modalidad.PRESENCIAL) is Resultado.Exito)
            assertNotNull(repo.obtenerDetallePorId(id)!!.cita.boxId)
            db.servicioDao().actualizar(db.servicioDao().obtenerPorId(1)!!.copy(permiteOnline = false))
            assertEquals(ErrorAgenda.ModalidadNoDisponible, (repo.reprogramar(id, hora, Modalidad.ONLINE) as Resultado.Error).error)
        } finally { db.close() }
    }

}
