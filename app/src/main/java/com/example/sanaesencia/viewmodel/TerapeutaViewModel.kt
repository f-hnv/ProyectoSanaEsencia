package com.example.sanaesencia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.local.entity.UsuarioEntity
import com.example.sanaesencia.data.local.relation.CitaConDetalle
import com.example.sanaesencia.data.repository.CuentaRepository
import com.example.sanaesencia.domain.model.PeriodoAgenda
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgendaTerapeutaState(
    val usuario: UsuarioEntity? = null,
    val periodo: PeriodoAgenda = PeriodoAgenda.desde(),
    val citas: List<CitaConDetalle> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class TerapeutaViewModel(app: Application) : AndroidViewModel(app) {
    private val cuenta = CuentaRepository(app)
    private val _estado = MutableStateFlow(AgendaTerapeutaState())
    val estado = _estado.asStateFlow()
    private var consulta: Job? = null
    init { cargar() }

    fun cambiarMes(desplazamiento: Int) {
        _estado.update { it.copy(periodo = it.periodo.desplazar(desplazamiento), citas = emptyList()) }
        cargar()
    }

    fun cargar() {
        consulta?.cancel()
        _estado.update { it.copy(cargando = true, error = null) }
        consulta = viewModelScope.launch {
            val usuario = cuenta.actual()
            val id = usuario?.especialistaId
            if (usuario?.rol != "ESPECIALISTA" || id == null) {
                _estado.update { it.copy(cargando = false, error = "Tu cuenta no tiene un perfil de especialista asignado. Consulta con administración.") }
                return@launch
            }
            val periodo = _estado.value.periodo
            _estado.update { it.copy(usuario = usuario) }
            // La consulta solo lee las citas del profesional autenticado y del mes seleccionado.
            cuenta.db.citaDao().observarMesEspecialista(id, periodo.inicio, periodo.fin)
                .catch { _estado.update { it.copy(cargando = false, error = "No se pudo cargar tu agenda. Reintenta.") } }
                .collect { citas -> _estado.update { it.copy(citas = citas, cargando = false) } }
        }
    }
}
