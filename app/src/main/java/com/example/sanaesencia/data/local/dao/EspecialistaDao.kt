package com.example.sanaesencia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EspecialistaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(especialista: EspecialistaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(especialistas: List<EspecialistaEntity>)

    @Update
    suspend fun actualizar(especialista: EspecialistaEntity)

    @Query("SELECT * FROM especialistas WHERE activo = 1 ORDER BY nombre")
    fun observarActivos(): Flow<List<EspecialistaEntity>>

    @Query("SELECT * FROM especialistas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): EspecialistaEntity?

    @Query("SELECT COUNT(*) FROM especialistas")
    suspend fun contar(): Int
}