package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.domain.model.SedeSanaesencia
import com.example.sanaesencia.nativo.ProveedorUbicacion
import com.example.sanaesencia.nativo.UbicacionResultado
import com.example.sanaesencia.utils.Geo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la obtención de ubicación. */
sealed interface EstadoUbicacion {
    data object Inicial : EstadoUbicacion               // aún no se ha pedido nada
    data object Cargando : EstadoUbicacion              // buscando señal → mostrar progreso
    data object PermisoDenegado : EstadoUbicacion       // → explicar para qué se usa + botón "Permitir" / abrir Ajustes
    data object UbicacionDesactivada : EstadoUbicacion  // → pedir que active el GPS en Ajustes
    data object NoDisponible : EstadoUbicacion          // timeout o sin señal → botón "Reintentar"
    data class Exito(
        val latitud: Double,
        val longitud: Double,
        val precisionM: Float?                          // Radio de precisión informado por el proveedor, expresado en metros.
    ) : EstadoUbicacion
}

/**
 * @property estado          ver EstadoUbicacion.
 * @property distanciaMetros distancia a la sede en metros; null hasta que haya Exito.
 * @property distanciaTexto  la misma distancia ya formateada ("850 m", "12,3 km"); null hasta que haya Exito.
 */
data class SedeUiState(
    val estado: EstadoUbicacion = EstadoUbicacion.Inicial,
    val distanciaMetros: Double? = null,
    val distanciaTexto: String? = null
)

/**
 * Traduce los resultados del proveedor de ubicación a estados de pantalla y calcula la distancia a la sede.
 * La vista solicita permisos; este ViewModel coordina la lectura y expone el resultado.
 */
class SedeViewModel(private val locationHelper: ProveedorUbicacion) : ViewModel() {

    private val _uiState = MutableStateFlow(SedeUiState())
    val uiState: StateFlow<SedeUiState> = _uiState.asStateFlow()

    val permisosRequeridos: Array<String> get() = locationHelper.permisosRequeridos

    fun tienePermiso(): Boolean = locationHelper.tienePermiso()

    fun onPermisoResultado(concedido: Boolean) {
        if (concedido) actualizarUbicacion()
        else _uiState.update { it.copy(estado = EstadoUbicacion.PermisoDenegado) }
    }

    fun actualizarUbicacion() {
        if (_uiState.value.estado is EstadoUbicacion.Cargando) return // evita lecturas simultáneas
        _uiState.update { it.copy(estado = EstadoUbicacion.Cargando) }

        viewModelScope.launch {
            _uiState.value = when (val resultado = locationHelper.obtenerUbicacionActual()) {
                is UbicacionResultado.Exito -> {
                    val metros = Geo.distanciaMetros(
                        resultado.latitud, resultado.longitud,
                        SedeSanaesencia.LATITUD, SedeSanaesencia.LONGITUD
                    )
                    SedeUiState(
                        estado = EstadoUbicacion.Exito(
                            resultado.latitud, resultado.longitud, resultado.precisionM
                        ),
                        distanciaMetros = metros,
                        distanciaTexto = Geo.formatearDistancia(metros)
                    )
                }
                UbicacionResultado.SinPermiso -> SedeUiState(EstadoUbicacion.PermisoDenegado)
                UbicacionResultado.UbicacionDesactivada -> SedeUiState(EstadoUbicacion.UbicacionDesactivada)
                UbicacionResultado.NoDisponible -> SedeUiState(EstadoUbicacion.NoDisponible)
            }
        }
    }
}

