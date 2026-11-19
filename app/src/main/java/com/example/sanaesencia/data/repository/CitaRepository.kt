package com.example.sanaesencia.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.CitaEntity
import com.example.sanaesencia.data.local.relation.CitaConDetalle
import com.example.sanaesencia.domain.model.ErrorAgenda
import com.example.sanaesencia.domain.model.EstadoCita
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.domain.model.Resultado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import com.example.sanaesencia.domain.model.HorariosAgenda

class CitaRepository(
    private val db: AppDatabase,
    // Reloj inyectable: permite probar la lógica con una hora fija.
    private val ahora: () -> Long = { System.currentTimeMillis() }
) {
    private val citaDao = db.citaDao()
    private val servicioDao = db.servicioDao()
    private val especialistaDao = db.especialistaDao()

    fun observarPorPaciente(email: String): Flow<List<CitaConDetalle>> =
        citaDao.observarPorPaciente(email)

    fun observarProximas(email: String): Flow<List<CitaConDetalle>> =
        citaDao.observarProximas(email, ahora())

    suspend fun agendar(
        emailPaciente: String,
        servicioId: Long,
        especialistaId: Long,
        fechaHoraInicio: Long,
        modalidad: Modalidad,
        notas: String = ""
    ): Resultado<Long> = db.withTransaction<Resultado<Long>> {
        // withTransaction: validar traslapes e insertar ocurre como una unidad atómica,
        // así dos agendamientos simultáneos no pueden colarse entre la validación y el insert.

        if (fechaHoraInicio <= ahora()) {
            return@withTransaction Resultado.Error(ErrorAgenda.FechaPasada)
        }

        val servicio = servicioDao.obtenerPorId(servicioId)?.takeIf { it.activo }
            ?: return@withTransaction Resultado.Error(ErrorAgenda.ServicioNoDisponible)

        if (fechaHoraInicio - ahora() < HorariosAgenda.ANTICIPACION_MS)
            return@withTransaction Resultado.Error(ErrorAgenda.AnticipacionInsuficiente)
        if (!HorariosAgenda.esBloqueValido(fechaHoraInicio, servicio.duracionMin))
            return@withTransaction Resultado.Error(ErrorAgenda.HorarioFueraAtencion)

        val especialista = especialistaDao.obtenerPorId(especialistaId)?.takeIf { it.activo }
            ?: return@withTransaction Resultado.Error(ErrorAgenda.EspecialistaNoDisponible)

        if ((modalidad == Modalidad.ONLINE && (!servicio.permiteOnline || !especialista.permiteOnline)) ||
            (modalidad == Modalidad.PRESENCIAL && !especialista.permitePresencial)) {
            return@withTransaction Resultado.Error(ErrorAgenda.ModalidadNoDisponible)
        }

        // Validación de traslapes de rango: [inicio, fin) contra citas no canceladas.
        val fin = fechaHoraInicio + servicio.duracionMin * 60_000L
        if (citaDao.buscarTraslapes(especialistaId, fechaHoraInicio, fin).isNotEmpty()) {
            return@withTransaction Resultado.Error(ErrorAgenda.HorarioOcupado)
        }

        val ocupacion = citaDao.ocupacion(fechaHoraInicio, fin)
        if (ocupacion.any { it.emailPaciente.equals(emailPaciente, true) })
            return@withTransaction Resultado.Error(ErrorAgenda.PacienteOcupado)
        val servicios = db.centroDao().serviciosDeEspecialista(especialistaId)
        if (servicios.isNotEmpty() && servicioId !in servicios)
            return@withTransaction Resultado.Error(ErrorAgenda.EspecialistaNoDisponible)
        val box = if (modalidad == Modalidad.PRESENCIAL) db.centroDao().boxesActivos().shuffled()
            .firstOrNull { b -> ocupacion.none { it.boxId == b.id } } else null
        if (modalidad == Modalidad.PRESENCIAL && box == null)
            return@withTransaction Resultado.Error(ErrorAgenda.SinBox)

        val cita = CitaEntity(
            emailPaciente = emailPaciente,
            servicioId = servicioId,
            especialistaId = especialistaId,
            fechaHoraInicio = fechaHoraInicio,
            duracionMin = servicio.duracionMin,
            modalidad = modalidad,
            notas = notas,
            boxId = box?.id,
            creadaEn = ahora(),
            actualizadaEn = ahora()
        )

        try {
            Resultado.Exito(citaDao.insertar(cita))
        } catch (e: SQLiteConstraintException) {
            // Las validaciones y la inserción se mantienen dentro de la misma transacción.
            Resultado.Error(ErrorAgenda.HorarioOcupado)
        }
    }

    suspend fun cancelar(citaId: Long): Resultado<Unit> = db.withTransaction {
        val cita = citaDao.obtenerDetallePorId(citaId)?.cita
            ?: return@withTransaction Resultado.Error(ErrorAgenda.CitaNoEncontrada)

        if (cita.estado != EstadoCita.PROGRAMADA) {
            return@withTransaction Resultado.Error(ErrorAgenda.EstadoInvalido)
        }
        if (cita.fechaHoraInicio - ahora() < HorariosAgenda.ANTICIPACION_MS)
            return@withTransaction Resultado.Error(ErrorAgenda.CambioFueraPlazo)

        citaDao.actualizarEstado(citaId, EstadoCita.CANCELADA, ahora())
        Resultado.Exito(Unit)
    }
    // Obtiene una cita específica para cargarla en la pantalla de edición
    suspend fun obtenerDetallePorId(citaId: Long): CitaConDetalle? {
        return citaDao.obtenerDetallePorId(citaId)
    }

    // Valida y actualiza la fecha de una cita existente
    suspend fun reprogramar(citaId: Long, nuevaFechaHora: Long, nuevaModalidad: Modalidad? = null): Resultado<Unit> = db.withTransaction {
        val cita = citaDao.obtenerDetallePorId(citaId)?.cita
            ?: return@withTransaction Resultado.Error(ErrorAgenda.CitaNoEncontrada)

        if (cita.estado != EstadoCita.PROGRAMADA) return@withTransaction Resultado.Error(ErrorAgenda.EstadoInvalido)
        if (nuevaFechaHora <= ahora()) return@withTransaction Resultado.Error(ErrorAgenda.FechaPasada)
        if (cita.fechaHoraInicio - ahora() < HorariosAgenda.ANTICIPACION_MS)
            return@withTransaction Resultado.Error(ErrorAgenda.CambioFueraPlazo)
        if (nuevaFechaHora - ahora() < HorariosAgenda.ANTICIPACION_MS)
            return@withTransaction Resultado.Error(ErrorAgenda.AnticipacionInsuficiente)
        if (!HorariosAgenda.esBloqueValido(nuevaFechaHora, cita.duracionMin))
            return@withTransaction Resultado.Error(ErrorAgenda.HorarioFueraAtencion)

        val modalidad = nuevaModalidad ?: cita.modalidad
        val especialista = especialistaDao.obtenerPorId(cita.especialistaId)
            ?: return@withTransaction Resultado.Error(ErrorAgenda.EspecialistaNoDisponible)
        if ((modalidad == Modalidad.ONLINE && (servicioDao.obtenerPorId(cita.servicioId)?.permiteOnline != true || !especialista.permiteOnline)) ||
            (modalidad == Modalidad.PRESENCIAL && !especialista.permitePresencial))
            return@withTransaction Resultado.Error(ErrorAgenda.ModalidadNoDisponible)

        val fin = nuevaFechaHora + cita.duracionMin * 60_000L
        val traslapes = citaDao.buscarTraslapes(cita.especialistaId, nuevaFechaHora, fin)

        // Verificamos que no choque con otra cita (ignorando la cita actual que estamos moviendo)
        if (traslapes.any { it.id != citaId }) {
            return@withTransaction Resultado.Error(ErrorAgenda.HorarioOcupado)
        }

        val ocupacion = citaDao.ocupacion(nuevaFechaHora, fin).filter { it.id != citaId }
        if (ocupacion.any { it.emailPaciente.equals(cita.emailPaciente, true) })
            return@withTransaction Resultado.Error(ErrorAgenda.PacienteOcupado)
        val box = if (modalidad == Modalidad.PRESENCIAL) db.centroDao().boxesActivos().shuffled()
            .firstOrNull { b -> ocupacion.none { it.boxId == b.id } } else null
        if (modalidad == Modalidad.PRESENCIAL && box == null)
            return@withTransaction Resultado.Error(ErrorAgenda.SinBox)
        citaDao.actualizar(cita.copy(fechaHoraInicio = nuevaFechaHora, modalidad = modalidad, boxId = box?.id, actualizadaEn = ahora()))
        Resultado.Exito(Unit)
    }

    fun observarHorariosDisponibles(
        dia: Long, especialistaId: Long, duracionMin: Int, citaEditandoId: Long? = null,
        emailPaciente: String? = null, modalidad: Modalidad = Modalidad.PRESENCIAL
    ): Flow<List<Long>> = combine(
        citaDao.observarTodaOcupacion(HorariosAgenda.inicioDia(dia), HorariosAgenda.siguienteDia(dia)),
        db.centroDao().observarBoxesActivos()
    ) { citas, boxes ->
        HorariosAgenda.bloquesDelDia(dia, duracionMin).filter { inicio ->
            val fin = inicio + duracionMin * 60_000L
            val ocupadas = citas.filter { it.id != citaEditandoId && it.fechaHoraInicio < fin &&
                it.fechaHoraInicio + it.duracionMin * 60_000L > inicio }
            inicio - ahora() >= HorariosAgenda.ANTICIPACION_MS &&
                ocupadas.none { it.especialistaId == especialistaId ||
                    (emailPaciente != null && it.emailPaciente.equals(emailPaciente, true)) } &&
                (modalidad == Modalidad.ONLINE || boxes.any { box -> ocupadas.none { it.boxId == box.id } })
        }
    }
}
