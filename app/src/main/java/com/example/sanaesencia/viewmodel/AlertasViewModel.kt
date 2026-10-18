package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.repository.AlertasRepository
import com.example.sanaesencia.domain.model.AlertaDesercion
import com.example.sanaesencia.domain.model.NivelRiesgo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * @property cargando        true hasta la primera lectura → progreso.
 * @property filtro          nivel activo; null = todos.
 * @property alertas         pacientes ya filtrados y ordenados de mayor a menor riesgo → lista.
 *                           Cada AlertaDesercion trae puntaje (0..100), nivel, motivos (textos listos) y métricas.
 * @property conteoPorNivel  cantidad de pacientes por nivel, SIN aplicar el filtro → chips/KPIs (siempre trae los 3 niveles).
 * @property totalPacientes  pacientes con historial evaluado.
 * @property error           mensaje si falló la carga; null si todo bien.
 */
data class AlertasUiState(
    val cargando: Boolean = true,
    val filtro: NivelRiesgo? = null,
    val alertas: List<AlertaDesercion> = emptyList(),
    val conteoPorNivel: Map<NivelRiesgo, Int> = emptyMap(),
    val totalPacientes: Int = 0,
    val error: String? = null
)

/**
 * Filtra las alertas de continuidad de tratamiento y expone sus contadores por nivel de riesgo.
 * Esta lógica aún no tiene una pantalla de administración conectada a la navegación.
 */
class AlertasViewModel(alertasRepository: AlertasRepository) : ViewModel() {

    private val filtro = MutableStateFlow<NivelRiesgo?>(null)

    val uiState: StateFlow<AlertasUiState> = combine(
        alertasRepository.observarAlertas(),
        filtro
    ) { todas, nivel ->
        AlertasUiState(
            cargando = false,
            filtro = nivel,
            alertas = if (nivel == null) todas else todas.filter { it.nivel == nivel },
            conteoPorNivel = NivelRiesgo.entries.associateWith { n -> todas.count { it.nivel == n } },
            totalPacientes = todas.size
        )
    }
        .catch { emit(AlertasUiState(cargando = false, error = "No se pudieron calcular las alertas")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertasUiState())

    fun filtrarPorNivel(nivel: NivelRiesgo?) {
        filtro.value = nivel
    }
}

