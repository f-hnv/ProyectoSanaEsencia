package com.example.sanaesencia

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.repository.CitaRepository
import com.example.sanaesencia.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class CitaHorariosTest {
    @Test fun migracionConservaDatosYLiberaLaRestriccionDeHorasCanceladas() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val archivo = java.io.File(context.cacheDir, "agenda-migracion-${System.nanoTime()}.db")
        val anterior = Room.databaseBuilder(context, AppDatabase::class.java, archivo.absolutePath).build()
        try {
            anterior.servicioDao().insertarTodos(listOf(ServicioEntity(id=1, nombre="Prueba", categoria="Prueba", descripcion="", duracionMin=50, precioClp=1, permiteOnline=false)))
            anterior.especialistaDao().insertarTodos(listOf(EspecialistaEntity(id=1,nombre="Profesional",especialidad="Prueba")))
            anterior.citaDao().insertar(CitaEntity(id=1,emailPaciente="migration@example.com",servicioId=1,especialistaId=1,
                fechaHoraInicio=fecha(20,10),duracionMin=50,modalidad=Modalidad.PRESENCIAL,estado=EstadoCita.CANCELADA))
            // Reconstruir el único cambio de esquema de v1: su índice era UNIQUE.
            anterior.openHelper.writableDatabase.execSQL("DROP INDEX index_citas_especialistaId_fechaHoraInicio")
            anterior.openHelper.writableDatabase.execSQL("CREATE UNIQUE INDEX index_citas_especialistaId_fechaHoraInicio ON citas(especialistaId, fechaHoraInicio)")
            anterior.openHelper.writableDatabase.execSQL("PRAGMA user_version = 1")
        } finally { anterior.close() }
        val migrada = Room.databaseBuilder(context,AppDatabase::class.java,archivo.absolutePath)
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            assertEquals(1L,migrada.citaDao().obtenerDetallePorId(1)!!.cita.id)
            val repo = CitaRepository(migrada) { fecha(19,8) }
            assertTrue(repo.agendar("nuevo@example.com",1,1,fecha(20,10),Modalidad.PRESENCIAL) is Resultado.Exito)
            assertEquals(EstadoCita.CANCELADA,migrada.citaDao().obtenerDetallePorId(1)!!.cita.estado)
        } finally {
            migrada.close()
            context.deleteDatabase(archivo.absolutePath)
        }
    }

    private fun fecha(dia: Int, hora: Int, minuto: Int = 0): Long = Calendar.getInstance(HorariosAgenda.zona).apply {
        clear(); set(2026, Calendar.OCTOBER, dia, hora, minuto)
    }.timeInMillis

    @Test fun horasOcupadasSeOcultanYCanceladasSePuedenVolverAReservar() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            db.servicioDao().insertarTodos(listOf(ServicioEntity(id=1, nombre="Prueba", categoria="Prueba", descripcion="", duracionMin=50, precioClp=1, permiteOnline=false)))
            db.especialistaDao().insertarTodos(listOf(EspecialistaEntity(id=1, nombre="Uno", especialidad="Prueba"), EspecialistaEntity(id=2, nombre="Dos", especialidad="Prueba")))
            val repo = CitaRepository(db) { fecha(19, 8) }
            val hora = fecha(20, 10)
            val reserva = repo.agendar("test@example.com", 1, 1, hora, Modalidad.PRESENCIAL) as Resultado.Exito
            assertFalse(hora in repo.observarHorariosDisponibles(hora, 1, 50).first())
            assertTrue(hora in repo.observarHorariosDisponibles(hora, 2, 50).first())
            assertTrue(hora in repo.observarHorariosDisponibles(hora, 1, 50, reserva.datos).first())
            assertEquals(ErrorAgenda.HorarioOcupado, (repo.agendar("otro@example.com",1,1,hora,Modalidad.PRESENCIAL) as Resultado.Error).error)
            assertTrue(repo.cancelar(reserva.datos) is Resultado.Exito)
            assertTrue(hora in repo.observarHorariosDisponibles(hora, 1, 50).first())
            assertTrue(repo.agendar("otro@example.com",1,1,hora,Modalidad.PRESENCIAL) is Resultado.Exito)
            assertEquals(ErrorAgenda.HorarioFueraAtencion, (repo.agendar("test@example.com",1,1,fecha(20,10,23),Modalidad.PRESENCIAL) as Resultado.Error).error)
            assertEquals(ErrorAgenda.AnticipacionInsuficiente, (repo.agendar("test@example.com",1,1,fecha(19,10),Modalidad.PRESENCIAL) as Resultado.Error).error)
            val tarde = CitaRepository(db) { fecha(20, 9) }
            val actual = repo.observarPorPaciente("otro@example.com").first().single().cita
            assertEquals(ErrorAgenda.CambioFueraPlazo, (tarde.cancelar(actual.id) as Resultado.Error).error)
            assertEquals(ErrorAgenda.CambioFueraPlazo, (tarde.reprogramar(actual.id,fecha(21,10)) as Resultado.Error).error)
        } finally { db.close() }
    }
}
