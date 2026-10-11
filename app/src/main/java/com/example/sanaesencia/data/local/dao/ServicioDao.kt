package com.example.sanaesencia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sanaesencia.data.local.entity.ServicioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServicioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(servicio: ServicioEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(servicios: List<ServicioEntity>)

    @Update
    suspend fun actualizar(servicio: ServicioEntity)

    @Query("SELECT * FROM servicios WHERE activo = 1 ORDER BY categoria, nombre")
    fun observarActivos(): Flow<List<ServicioEntity>>

    @Query("SELECT * FROM servicios WHERE activo = 1 AND categoria = :categoria ORDER BY nombre")
    fun observarPorCategoria(categoria: String): Flow<List<ServicioEntity>>

    @Query("SELECT DISTINCT categoria FROM servicios WHERE activo = 1 ORDER BY categoria")
    fun observarCategorias(): Flow<List<String>>

    @Query("SELECT * FROM servicios WHERE id = :id")
    suspend fun obtenerPorId(id: Long): ServicioEntity?

    @Query("SELECT COUNT(*) FROM servicios")
    suspend fun contar(): Int
}