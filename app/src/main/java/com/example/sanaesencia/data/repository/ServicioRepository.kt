package com.example.sanaesencia.data.repository

import com.example.sanaesencia.data.local.dao.ServicioDao
import com.example.sanaesencia.data.local.entity.ServicioEntity
import kotlinx.coroutines.flow.Flow

class ServicioRepository(private val dao: ServicioDao) {

    fun observarServicios(): Flow<List<ServicioEntity>> = dao.observarActivos()

    fun observarPorCategoria(categoria: String): Flow<List<ServicioEntity>> =
        dao.observarPorCategoria(categoria)

    fun observarCategorias(): Flow<List<String>> = dao.observarCategorias()

    suspend fun obtenerPorId(id: Long): ServicioEntity? = dao.obtenerPorId(id)
}