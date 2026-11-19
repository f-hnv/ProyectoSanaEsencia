package com.example.sanaesencia

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.local.seed.*
import com.example.sanaesencia.data.repository.*
import com.example.sanaesencia.domain.model.*
import com.example.sanaesencia.viewmodel.AgendaViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class CatalogoCentroTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun temporal() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .addCallback(AppDatabase.INTEGRIDAD).build()

    @Test fun importaEquipoServiciosYAgendasPausadasSinCrearCuentasNiDuplicar() = runBlocking {
        val db = temporal()
        try {
            CatalogoCentro.instalar(db)
            CatalogoCentro.instalar(db)
            assertEquals(30, db.especialistaDao().contar())
            assertEquals(23, db.especialistaDao().observarActivos().first().size)
            assertEquals(12, db.servicioDao().contar())
            assertTrue(db.servicioDao().observarActivos().first().all { it.duracionMin == 60 && it.precioClp == 30_000 })
            assertTrue(db.centroDao().usuarios().first().isEmpty())
            assertTrue(db.citaDao().observarTodas().first().isEmpty())
            val infantil = db.servicioDao().obtenerPorNombre("Psicología infantojuvenil")!!
            assertEquals(setOf("Samantha Bastías", "Sebastián Olivares"),
                db.especialistaDao().observarParaServicio(infantil.id).first().map { it.nombre }.toSet())
            val maria = db.especialistaDao().obtenerPorNombre("Maria Ignacia")!!
            assertEquals(3, db.centroDao().serviciosDeEspecialista(maria.id).size)
            assertFalse(db.especialistaDao().obtenerPorNombre("Nataly Godoy")!!.activo)
        } finally { db.close() }
    }

    @Test fun actualizarCatalogoConservaReservasViejasYDatosPersonalizados() = runBlocking {
        val db = temporal()
        try {
            SeedData.poblarSiVacia(db)
            val citas = db.citaDao().observarTodas().first().map { it.cita }
            CatalogoCentro.instalar(db)
            assertEquals(citas, db.citaDao().observarTodas().first().map { it.cita })
            assertFalse(db.especialistaDao().obtenerPorId(1)!!.activo)
            assertFalse(db.servicioDao().obtenerPorId(1)!!.activo)
            val original = db.servicioDao().obtenerPorNombre("Fonoaudiología")!!
            db.servicioDao().actualizar(original.copy(precioClp = 32_000))
            CatalogoCentro.instalar(db)
            assertEquals(32_000, db.servicioDao().obtenerPorId(original.id)!!.precioClp)
            assertEquals(32, db.especialistaDao().contar())
        } finally { db.close() }
    }

    @Test fun profesionalSoloOnlineSeleccionaOnlineYRechazaReservaPresencial() = runBlocking {
        val db = temporal()
        var vm: AgendaViewModel? = null
        try {
            CatalogoCentro.instalar(db)
            val servicio = db.servicioDao().obtenerPorNombre("Medicina general y salud mental")!!
            val camila = db.especialistaDao().obtenerPorNombre("Dra. Camila Tabilo")!!
            val ahora = PeriodoAgenda(2026, 12).inicio
            val hora = HorariosAgenda.calendario(ahora).apply { set(Calendar.DAY_OF_MONTH, 2); set(Calendar.HOUR_OF_DAY, 10) }.timeInMillis
            val repo = CitaRepository(db) { ahora }
            assertEquals(ErrorAgenda.ModalidadNoDisponible,
                (repo.agendar("qa@test.cl", servicio.id, camila.id, hora, Modalidad.PRESENCIAL) as Resultado.Error).error)
            val id = (repo.agendar("qa@test.cl", servicio.id, camila.id, hora, Modalidad.ONLINE) as Resultado.Exito).datos
            assertNull(repo.obtenerDetallePorId(id)!!.cita.boxId)
            assertEquals(ErrorAgenda.ModalidadNoDisponible,
                (repo.reprogramar(id, hora, Modalidad.PRESENCIAL) as Resultado.Error).error)
            val agenda = AgendaViewModel(repo, ServicioRepository(db.servicioDao()), EspecialistaRepository(db.especialistaDao()), "otro@test.cl")
            vm = agenda
            agenda.onServicioSeleccionado(servicio.id)
            withTimeout(10_000) { agenda.uiState.first { it.servicioSeleccionado?.id == servicio.id && it.especialistas.any { p -> p.id == camila.id } } }
            agenda.onEspecialistaSeleccionado(camila.id)
            assertEquals(Modalidad.ONLINE, agenda.uiState.value.modalidad)
            agenda.onModalidadSeleccionada(Modalidad.PRESENCIAL)
            assertEquals(Modalidad.ONLINE, agenda.uiState.value.modalidad)
        } finally {
            vm?.viewModelScope?.coroutineContext?.get(Job)?.cancelAndJoin()
            db.close()
        }
    }

    @Test fun migracionDesdeVersionTresConservaLosPerfilesYReservas() = runBlocking {
        val archivo = java.io.File(context.cacheDir, "catalogo-migracion-${System.nanoTime()}.db")
        val anterior = Room.databaseBuilder(context, AppDatabase::class.java, archivo.absolutePath).addCallback(AppDatabase.INTEGRIDAD).build()
        try {
            SeedData.poblarSiVacia(anterior)
            anterior.openHelper.writableDatabase.apply {
                execSQL("ALTER TABLE especialistas DROP COLUMN permitePresencial")
                execSQL("ALTER TABLE especialistas DROP COLUMN permiteOnline")
                execSQL("PRAGMA user_version = 3")
            }
        } finally { anterior.close() }
        val migrada = Room.databaseBuilder(context, AppDatabase::class.java, archivo.absolutePath)
            .addMigrations(AppDatabase.MIGRATION_3_4).addCallback(AppDatabase.INTEGRIDAD).build()
        try {
            assertEquals("Dra. Paula Demo", migrada.especialistaDao().obtenerPorId(1)!!.nombre)
            assertTrue(migrada.especialistaDao().obtenerPorId(1)!!.permitePresencial)
            assertTrue(migrada.especialistaDao().obtenerPorId(1)!!.permiteOnline)
            assertFalse(migrada.citaDao().observarTodas().first().isEmpty())
        } finally { migrada.close(); context.deleteDatabase(archivo.absolutePath) }
    }
}
