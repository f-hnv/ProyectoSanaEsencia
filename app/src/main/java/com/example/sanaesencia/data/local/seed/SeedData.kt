package com.example.sanaesencia.data.local.seed

import androidx.room.withTransaction
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.CitaEntity
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import com.example.sanaesencia.data.local.entity.ServicioEntity
import com.example.sanaesencia.domain.model.EstadoCita
import com.example.sanaesencia.domain.model.Modalidad
import java.util.Calendar

// Datos 100% ficticios para desarrollo y pruebas de UI.
object SeedData {

    const val EMAIL_DEMO = "paciente.demo@example.com"

    val servicios = listOf(
        ServicioEntity(
            id = 1, nombre = "Psicoterapia individual", categoria = "Psicoterapia",
            descripcion = "Sesión individual de acompañamiento psicológico.",
            duracionMin = 50, precioClp = 35_000, permiteOnline = true
        ),
        ServicioEntity(
            id = 2, nombre = "Sesión de Reiki", categoria = "Terapias complementarias",
            descripcion = "Terapia energética orientada a la relajación profunda.",
            duracionMin = 45, precioClp = 25_000, permiteOnline = false
        ),
        ServicioEntity(
            id = 3, nombre = "Taller de Mindfulness", categoria = "Bienestar",
            descripcion = "Práctica guiada de atención plena para manejar el estrés.",
            duracionMin = 60, precioClp = 20_000, permiteOnline = true
        )
    )

    val especialistas = listOf(
        EspecialistaEntity(
            id = 1, nombre = "Dra. Paula Demo", especialidad = "Psicología clínica",
            descripcion = "Perfil ficticio para pruebas."
        ),
        EspecialistaEntity(
            id = 2, nombre = "Ter. Mateo Demo", especialidad = "Terapias complementarias",
            descripcion = "Perfil ficticio para pruebas."
        )
    )

    // Las citas se calculan respecto a "ahora" para que siempre haya una cita futura y una pasada.
    fun citas(ahora: Long): List<CitaEntity> = listOf(
        CitaEntity(
            emailPaciente = EMAIL_DEMO, servicioId = 1, especialistaId = 1,
            fechaHoraInicio = diaHora(ahora, -7, 10), duracionMin = 50,
            modalidad = Modalidad.ONLINE, estado = EstadoCita.COMPLETADA
        ),
        CitaEntity(
            emailPaciente = EMAIL_DEMO, servicioId = 1, especialistaId = 1,
            fechaHoraInicio = diaHora(ahora, 1, 10), duracionMin = 50,
            modalidad = Modalidad.PRESENCIAL, estado = EstadoCita.PROGRAMADA
        ),
        CitaEntity(
            emailPaciente = EMAIL_DEMO, servicioId = 2, especialistaId = 2,
            fechaHoraInicio = diaHora(ahora, 3, 16), duracionMin = 45,
            modalidad = Modalidad.PRESENCIAL, estado = EstadoCita.PROGRAMADA
        )
    )

    // Solo puebla si la tabla de servicios está vacía (primera ejecución).
    suspend fun poblarSiVacia(db: AppDatabase, ahora: Long = System.currentTimeMillis()) {
        if (db.servicioDao().contar() > 0) return
        db.withTransaction {
            db.servicioDao().insertarTodos(servicios)
            db.especialistaDao().insertarTodos(especialistas)
            (citas(ahora) + citasAlertas(ahora)).forEach { db.citaDao().insertar(it) }
        }
    }

    private fun diaHora(base: Long, offsetDias: Int, hora: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = base
            add(Calendar.DAY_OF_YEAR, offsetDias)
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // Pacientes ficticios con distintos niveles de riesgo (ALTO ≈ 78 pts, MEDIO ≈ 40 pts).
    fun citasAlertas(ahora: Long): List<CitaEntity> {
        fun cita(email: String, especialistaId: Long, dias: Int, hora: Int, estado: EstadoCita) =
            CitaEntity(
                emailPaciente = email, servicioId = 1, especialistaId = especialistaId,
                fechaHoraInicio = diaHora(ahora, dias, hora), duracionMin = 50,
                modalidad = Modalidad.PRESENCIAL, estado = estado
            )
        return listOf(
            cita("riesgo.alto.demo@example.com", 2, -75, 9, EstadoCita.COMPLETADA),
            cita("riesgo.alto.demo@example.com", 2, -60, 9, EstadoCita.NO_ASISTIO),
            cita("riesgo.alto.demo@example.com", 2, -50, 9, EstadoCita.NO_ASISTIO),
            cita("riesgo.alto.demo@example.com", 2, -45, 9, EstadoCita.CANCELADA),
            cita("riesgo.medio.demo@example.com", 1, -50, 11, EstadoCita.COMPLETADA),
            cita("riesgo.medio.demo@example.com", 1, -40, 11, EstadoCita.CANCELADA),
            cita("riesgo.medio.demo@example.com", 1, -33, 11, EstadoCita.CANCELADA)
        )
    }
}
