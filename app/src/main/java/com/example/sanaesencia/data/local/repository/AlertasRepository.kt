package com.example.sanaesencia.data.repository

import com.example.sanaesencia.data.local.dao.CitaDao
import com.example.sanaesencia.domain.analytics.CalculadorRiesgo
import com.example.sanaesencia.domain.model.AlertaDesercion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AlertasRepository(
    private val citaDao: CitaDao,
    private val ahora: () -> Long = { System.currentTimeMillis() }
) {
    /** Alertas de todos los pacientes, de mayor a menor riesgo. Se actualiza sola al cambiar las citas. */
    fun observarAlertas(): Flow<List<AlertaDesercion>> =
        citaDao.observarResumenPacientes(ahora())
            .map { resumenes ->
                val t = ahora()
                resumenes.mapNotNull { CalculadorRiesgo.evaluar(it, t) }
                    .sortedByDescending { it.puntaje }
            }
            .flowOn(Dispatchers.Default) // el cálculo no ocupa el hilo principal
}

