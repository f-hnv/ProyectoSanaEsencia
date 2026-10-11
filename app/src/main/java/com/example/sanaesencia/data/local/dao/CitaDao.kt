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

@Dao
interface CitaDao {

    // ABORT: si ya existe una cita del especialista a esa hora exacta (índice único),
    // lanza SQLiteConstraintException y el Repository la traduce a un error de negocio.
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

    // Base para las alertas de deserción (ratios de cancelaciones / inasistencias).
    @Query("SELECT COUNT(*) FROM citas WHERE emailPaciente = :email AND estado = :estado")
    suspend fun contarPorEstado(email: String, estado: EstadoCita): Int
}