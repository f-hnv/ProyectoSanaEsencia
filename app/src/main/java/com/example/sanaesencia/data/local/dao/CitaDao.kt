package com.example.sanaesencia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.sanaesencia.data.local.entity.CitaEntity
import com.example.sanaesencia.data.local.relation.CitaConDetalle
import com.example.sanaesencia.domain.model.EstadoCita
import kotlinx.coroutines.flow.Flow
import com.example.sanaesencia.data.local.relation.ResumenPaciente



@Dao
interface CitaDao {

    // Los traslapes se validan dentro de una transacción en el repositorio.

    @Query(
        """SELECT emailPaciente,
                  SUM(CASE WHEN estado = 'COMPLETADA' THEN 1 ELSE 0 END) AS completadas,
                  SUM(CASE WHEN estado = 'CANCELADA' THEN 1 ELSE 0 END) AS canceladas,
                  SUM(CASE WHEN estado = 'NO_ASISTIO' THEN 1 ELSE 0 END) AS inasistencias,
                  SUM(CASE WHEN estado = 'PROGRAMADA' AND fechaHoraInicio >= :ahora THEN 1 ELSE 0 END) AS futuras,
                  MAX(CASE WHEN estado = 'COMPLETADA' THEN fechaHoraInicio END) AS ultimaAtencion,
                  MIN(fechaHoraInicio) AS primeraCita
           FROM citas
           GROUP BY emailPaciente"""
    )
    fun observarResumenPacientes(ahora: Long): Flow<List<ResumenPaciente>>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(cita: CitaEntity): Long

    @Update
    suspend fun actualizar(cita: CitaEntity)

    @Query("UPDATE citas SET estado = :estado, actualizadaEn = :ahora WHERE id = :id")
    suspend fun actualizarEstado(id: Long, estado: EstadoCita, ahora: Long = System.currentTimeMillis())

    @Transaction
    @Query("SELECT * FROM citas WHERE emailPaciente = :email ORDER BY fechaHoraInicio DESC")
    fun observarPorPaciente(email: String): Flow<List<CitaConDetalle>>

    @Transaction
    @Query(
        """SELECT * FROM citas
           WHERE emailPaciente = :email AND estado = :estado AND fechaHoraInicio >= :desde
           ORDER BY fechaHoraInicio ASC"""
    )
    fun observarProximas(
        email: String,
        desde: Long,
        estado: EstadoCita = EstadoCita.PROGRAMADA
    ): Flow<List<CitaConDetalle>>

    @Transaction
    @Query("SELECT * FROM citas WHERE id = :id")
    suspend fun obtenerDetallePorId(id: Long): CitaConDetalle?

    // Citas activas del especialista que se traslapan con [inicio, fin). Se usa para validar la agenda.
    @Query(
        """SELECT * FROM citas
           WHERE especialistaId = :especialistaId
             AND estado != :excluir
             AND fechaHoraInicio < :fin
             AND (fechaHoraInicio + duracionMin * 60000) > :inicio"""
    )
    suspend fun buscarTraslapes(
        especialistaId: Long,
        inicio: Long,
        fin: Long,
        excluir: EstadoCita = EstadoCita.CANCELADA
    ): List<CitaEntity>

    @Query("""SELECT * FROM citas WHERE especialistaId = :especialistaId
        AND estado != 'CANCELADA' AND fechaHoraInicio < :fin
        AND (fechaHoraInicio + duracionMin * 60000) > :inicio""")
    fun observarOcupacion(especialistaId: Long, inicio: Long, fin: Long): Flow<List<CitaEntity>>

    // Base para las alertas de deserción (ratios de cancelaciones / inasistencias).
    @Query("SELECT COUNT(*) FROM citas WHERE emailPaciente = :email AND estado = :estado")
    suspend fun contarPorEstado(email: String, estado: EstadoCita): Int
}
