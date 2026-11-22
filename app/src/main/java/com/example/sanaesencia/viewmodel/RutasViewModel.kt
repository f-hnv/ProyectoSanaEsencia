package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.repository.ErrorRuta
import com.example.sanaesencia.data.repository.RecorridoSede
import com.example.sanaesencia.data.repository.RutasRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RutaUiState {
    data object Inicial: RutaUiState
    data object Cargando: RutaUiState
    data class Exito(val recorrido: RecorridoSede): RutaUiState
    data class Error(val mensaje: String): RutaUiState
}

class RutasViewModel(private val repository: RutasRepository): ViewModel() {
    private val _estado = MutableStateFlow<RutaUiState>(RutaUiState.Inicial)
    val estado = _estado.asStateFlow()
    private var trabajo: Job? = null
    fun limpiar() {
        trabajo?.cancel()
        _estado.value = RutaUiState.Inicial
    }
    fun consultar(latitud: Double, longitud: Double) {
        if (_estado.value is RutaUiState.Cargando) return
        _estado.value = RutaUiState.Cargando
        trabajo = viewModelScope.launch {
            try { _estado.value = RutaUiState.Exito(repository.consultar(latitud, longitud)) }
            catch (e: CancellationException) { throw e }
            catch (e: ErrorRuta) { _estado.value = RutaUiState.Error(e.message) }
        }
    }
}
