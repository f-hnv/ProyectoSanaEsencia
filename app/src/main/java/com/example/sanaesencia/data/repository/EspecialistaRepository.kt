package com.example.sanaesencia.data.repository

import com.example.sanaesencia.data.local.dao.EspecialistaDao
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class EspecialistaRepository(private val dao: EspecialistaDao, private val catalogo:(()->CatalogoRepository)?=null) {
    fun observarParaServicio(servicioId: Long?):Flow<List<EspecialistaEntity>> = catalogo?.invoke()?.let { remoto ->
        remoto.estado.map { estado -> estado.especialistas.filter {
            servicioId==null || servicioId in estado.serviciosPorEspecialista[it.id].orEmpty()
        } }.onStart { remoto.exigirVigente() }
    } ?: dao.observarParaServicio(servicioId)


    fun observarEspecialistas(): Flow<List<EspecialistaEntity>> = observarParaServicio(null)

    suspend fun obtenerPorId(id: Long): EspecialistaEntity? {
        val remoto=catalogo?.invoke() ?: return dao.obtenerPorId(id)
        remoto.exigirVigente()
        return remoto.estado.value.especialistas.find { it.id==id }
    }
}
