package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.local.entity.ServicioEntity
import com.example.sanaesencia.data.local.relation.CitaConDetalle
import com.example.sanaesencia.data.repository.CitaRepository
import com.example.sanaesencia.data.repository.ServicioRepository
import com.example.sanaesencia.domain.model.ErrorAgenda
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.domain.model.Resultado
import com.example.sanaesencia.utils.ErroresFormularioAgenda
import com.example.sanaesencia.utils.ValidacionesAgenda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EstadoOperacion {
    data object Inactivo : EstadoOperacion
    data object Cargando : EstadoOperacion
    data class Exito(val mensaje: String, val citaId: Long? = null) : EstadoOperacion
    data class Error(val error: ErrorAgenda) : EstadoOperacion
}

data class AgendaUiState(
    val citaEditandoId: Long? = null, // NUEVO: Identificador de edición
    val servicioSeleccionado: ServicioEntity? = null,
    val especialistaId: Long? = null,
    val fechaHoraInicio: Long? = null,
    val modalidad: Modalidad = Modalidad.PRESENCIAL,
    val notas: String = "",
    val errores: ErroresFormularioAgenda = ErroresFormularioAgenda(),
    val operacion: EstadoOperacion = EstadoOperacion.Inactivo,
    val proximasCitas: List<CitaConDetalle> = emptyList(),
    val todasLasCitas: List<CitaConDetalle> = emptyList()
)

class AgendaViewModel(
    private val citaRepository: CitaRepository,
    private val servicioRepository: ServicioRepository,
    private val emailPaciente: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgendaUiState())
    val uiState: StateFlow<AgendaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            citaRepository.observarProximas(emailPaciente).collect { citas ->
                _uiState.update { it.copy(proximasCitas = citas) }
            }
        }
        viewModelScope.launch {
            citaRepository.observarPorPaciente(emailPaciente).collect { citas ->
                _uiState.update { it.copy(todasLasCitas = citas) }
            }
        }
    }

    // Carga los datos de la base de datos para mostrarlos en el formulario
    fun prepararEdicion(citaId: Long) {
        viewModelScope.launch {
            val citaDetalle = citaRepository.obtenerDetallePorId(citaId) ?: return@launch
            _uiState.update {
                it.copy(
                    citaEditandoId = citaId,
                    servicioSeleccionado = citaDetalle.servicio,
                    especialistaId = citaDetalle.cita.especialistaId,
                    fechaHoraInicio = citaDetalle.cita.fechaHoraInicio,
                    modalidad = citaDetalle.cita.modalidad,
                    notas = citaDetalle.cita.notas ?: "",
                    errores = ErroresFormularioAgenda()
                )
            }
        }
    }

    fun onServicioSeleccionado(servicioId: Long) {
        viewModelScope.launch {
            val servicio = servicioRepository.obtenerPorId(servicioId)
            _uiState.update {
                it.copy(
                    servicioSeleccionado = servicio,
                    modalidad = if (servicio?.permiteOnline == false) Modalidad.PRESENCIAL else it.modalidad,
                    errores = it.errores.copy(servicio = null, modalidad = null)
                )
            }
        }
    }

    fun onEspecialistaSeleccionado(especialistaId: Long) {
        _uiState.update { it.copy(especialistaId = especialistaId, errores = it.errores.copy(especialista = null)) }
    }

    fun onFechaHoraSeleccionada(epochMillis: Long) {
        _uiState.update { it.copy(fechaHoraInicio = epochMillis, errores = it.errores.copy(fechaHora = null)) }
    }

    fun confirmarAgendamiento() {
        val actual = _uiState.value
        if (actual.operacion is EstadoOperacion.Cargando) return

        val servicioId = actual.servicioSeleccionado?.id
        val especialistaId = actual.especialistaId
        val fechaHora = actual.fechaHoraInicio

        val errores = ValidacionesAgenda.validar(servicioId, especialistaId, fechaHora)
        if (errores.hayErrores || servicioId == null || especialistaId == null || fechaHora == null) {
            _uiState.update { it.copy(errores = errores) }
            return
        }

        val ahora = System.currentTimeMillis()
        val doceHorasMilis = 12 * 60 * 60 * 1000L
        if (fechaHora - ahora < doceHorasMilis) {
            _uiState.update { it.copy(errores = it.errores.copy(fechaHora = "Debes agendar o modificar con al menos 12 horas de anticipación")) }
            return
        }

        _uiState.update { it.copy(errores = ErroresFormularioAgenda(), operacion = EstadoOperacion.Cargando) }

        viewModelScope.launch {
            // Decidimos dinámicamente si hacemos INSERT o UPDATE
            val resultado = if (actual.citaEditandoId != null) {
                citaRepository.reprogramar(actual.citaEditandoId, fechaHora)
            } else {
                citaRepository.agendar(emailPaciente, servicioId, especialistaId, fechaHora, actual.modalidad, actual.notas.trim())
            }

            when (resultado) {
                is Resultado.Exito -> _uiState.update {
                    it.copy(
                        operacion = EstadoOperacion.Exito(if (actual.citaEditandoId != null) "Cita reprogramada con éxito" else "Cita agendada correctamente"),
                        servicioSeleccionado = null, especialistaId = null, fechaHoraInicio = null, citaEditandoId = null,
                        modalidad = Modalidad.PRESENCIAL, notas = ""
                    )
                }
                is Resultado.Error -> _uiState.update {
                    it.copy(operacion = EstadoOperacion.Error(resultado.error), errores = erroresDeCampo(resultado.error, it.errores))
                }
            }
        }
    }

    fun cancelarCita(citaId: Long) {
        if (_uiState.value.operacion is EstadoOperacion.Cargando) return
        _uiState.update { it.copy(operacion = EstadoOperacion.Cargando) }
        viewModelScope.launch {
            val nuevaOperacion = when (val resultado = citaRepository.cancelar(citaId)) {
                is Resultado.Exito -> EstadoOperacion.Exito("Cita cancelada correctamente", citaId)
                is Resultado.Error -> EstadoOperacion.Error(resultado.error)
            }
            _uiState.update { it.copy(operacion = nuevaOperacion) }
        }
    }

    fun resultadoConsumido() {
        _uiState.update { it.copy(operacion = EstadoOperacion.Inactivo) }
    }

    private fun erroresDeCampo(error: ErrorAgenda, base: ErroresFormularioAgenda) = when (error) {
        ErrorAgenda.FechaPasada, ErrorAgenda.HorarioOcupado -> base.copy(fechaHora = error.mensaje)
        ErrorAgenda.ServicioNoDisponible -> base.copy(servicio = error.mensaje)
        ErrorAgenda.EspecialistaNoDisponible -> base.copy(especialista = error.mensaje)
        ErrorAgenda.ModalidadNoDisponible -> base.copy(modalidad = error.mensaje)
        else -> base
    }
}