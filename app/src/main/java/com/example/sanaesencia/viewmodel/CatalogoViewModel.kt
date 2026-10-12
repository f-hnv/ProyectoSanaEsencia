package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import com.example.sanaesencia.data.local.entity.ServicioEntity
import com.example.sanaesencia.data.repository.EspecialistaRepository
import com.example.sanaesencia.data.repository.ServicioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Estado de la pantalla de catálogo (servicios + especialistas).
 *
 * @property cargando             true hasta que Room entrega la primera lectura → mostrar indicador de carga.
 * @property categorias           categorías disponibles → chips/tabs de filtro.
 * @property categoriaSeleccionada categoría activa; null = "Todas".
 * @property servicios            servicios ya filtrados por la categoría activa → LazyColumn.
 * @property especialistas        especialistas activos → pantalla "Especialistas".
 * @property error                mensaje si falló la carga; null si todo bien.
 */
data class CatalogoUiState(
    val cargando: Boolean = true,
    val categorias: List<String> = emptyList(),
    val categoriaSeleccionada: String? = null,
    val servicios: List<ServicioEntity> = emptyList(),
    val especialistas: List<EspecialistaEntity> = emptyList(),
    val error: String? = null
)

/**
 * ═══ CONTRATO PARA LA UI (Compose) ═══
 *
 * OBSERVAR (un único StateFlow):
 *   val estado by catalogoVm.uiState.collectAsStateWithLifecycle()
 *   // requiere androidx.lifecycle:lifecycle-runtime-compose
 *   when {
 *       estado.cargando -> /* CircularProgressIndicator */
 *       estado.error != null -> /* mensaje de error */
 *       else -> /* estado.servicios, estado.especialistas */
 *   }
 *
 * EVENTOS (funciones que la UI llama):
 *   seleccionarCategoria(categoria: String?)  // null = mostrar todas
 *
 * La lista se actualiza sola (Room emite con Flow); no hay que "refrescar" a mano.
 */
class CatalogoViewModel(
    servicioRepository: ServicioRepository,
    especialistaRepository: EspecialistaRepository
) : ViewModel() {

    private val categoriaSeleccionada = MutableStateFlow<String?>(null)

    // Cada vez que cambia la categoría se cancela la consulta anterior y se abre la nueva.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val serviciosFiltrados = categoriaSeleccionada.flatMapLatest { categoria ->
        if (categoria == null) servicioRepository.observarServicios()
        else servicioRepository.observarPorCategoria(categoria)
    }

    val uiState: StateFlow<CatalogoUiState> = combine(
        servicioRepository.observarCategorias(),
        categoriaSeleccionada,
        serviciosFiltrados,
        especialistaRepository.observarEspecialistas()
    ) { categorias, seleccionada, servicios, especialistas ->
        CatalogoUiState(
            cargando = false,
            categorias = categorias,
            categoriaSeleccionada = seleccionada,
            servicios = servicios,
            especialistas = especialistas
        )
    }
        .catch { emit(CatalogoUiState(cargando = false, error = "No se pudo cargar el catálogo")) }
        // WhileSubscribed(5s): deja de consultar Room 5 s después de que la UI se va
        // (así sobrevive a rotaciones de pantalla sin reconsultar).
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogoUiState())

    fun seleccionarCategoria(categoria: String?) {
        categoriaSeleccionada.value = categoria
    }
}