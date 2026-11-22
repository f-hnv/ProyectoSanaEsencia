package com.example.sanaesencia.data.repository

import com.example.sanaesencia.data.local.dao.ServicioDao
import com.example.sanaesencia.data.local.entity.ServicioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class ServicioRepository(private val dao: ServicioDao, private val catalogo:(()->CatalogoRepository)?=null) {

    fun observarServicios(): Flow<List<ServicioEntity>> = catalogo?.invoke()?.let { remoto ->
        remoto.estado.map { it.servicios }.onStart { remoto.actualizar() }
    } ?: dao.observarActivos()

    fun observarPorCategoria(categoria: String): Flow<List<ServicioEntity>> =
        observarServicios().map { lista -> lista.filter { it.categoria==categoria } }

    fun observarCategorias(): Flow<List<String>> = observarServicios().map { lista -> lista.map { it.categoria }.distinct() }

    suspend fun obtenerPorId(id: Long): ServicioEntity? {
        val remoto=catalogo?.invoke() ?: return dao.obtenerPorId(id)
        remoto.exigirVigente()
        return remoto.estado.value.servicios.find { it.id==id }
    }
}
