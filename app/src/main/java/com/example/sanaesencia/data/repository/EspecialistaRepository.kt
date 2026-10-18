package com.example.sanaesencia.data.repository

import com.example.sanaesencia.data.local.dao.EspecialistaDao
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import kotlinx.coroutines.flow.Flow

class EspecialistaRepository(private val dao: EspecialistaDao) {

    fun observarEspecialistas(): Flow<List<EspecialistaEntity>> = dao.observarActivos()

    suspend fun obtenerPorId(id: Long): EspecialistaEntity? = dao.obtenerPorId(id)
}