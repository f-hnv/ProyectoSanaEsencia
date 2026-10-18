package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import com.example.sanaesencia.data.local.entity.ServicioEntity
import com.example.sanaesencia.data.local.relation.CitaConDetalle
import com.example.sanaesencia.data.repository.CitaRepository
import com.example.sanaesencia.data.repository.ServicioRepository
import com.example.sanaesencia.data.repository.EspecialistaRepository
import com.example.sanaesencia.domain.model.ErrorAgenda
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.domain.model.Resultado
import com.example.sanaesencia.utils.ErroresFormularioAgenda
import com.example.sanaesencia.utils.ValidacionesAgenda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.example.sanaesencia.domain.model.HorariosAgenda

sealed interface EstadoOperacion {
    data object Inactivo : EstadoOperacion
    data object Cargando : EstadoOperacion
    data class Exito(val mensaje: String, val citaId: Long? = null) : EstadoOperacion
    data class Error(val error: ErrorAgenda) : EstadoOperacion
}

data class AgendaUiState(
    val citaEditandoId: Long? = null, // Una cita existente activa la reprogramación; null representa una reserva nueva.
    val servicioSeleccionado: ServicioEntity? = null,
    val especialistaId: Long? = null,
    val especialistas: List<EspecialistaEntity> = emptyList(),
    val especialistaDeCita: EspecialistaEntity? = null,
    val cargandoEspecialistas: Boolean = true,
    val errorEspecialistas: String? = null,
    val fechaHoraInicio: Long? = null,
    val diaSeleccionado: Long? = null,
    val horariosDisponibles: List<Long> = emptyList(),
    val cargandoHorarios: Boolean = false,
    val errorHorarios: String? = null,
    val modalidad: Modalidad = Modalidad.PRESENCIAL,
    val notas: String = "",
    val errores: ErroresFormularioAgenda = ErroresFormularioAgenda(),
    val operacion: EstadoOperacion = EstadoOperacion.Inactivo,
    val proximasCitas: List<CitaConDetalle> = emptyList(),
    val todasLasCitas: List<CitaConDetalle> = emptyList()
)

/** Gestiona el formulario de reserva, observa la disponibilidad y delega la persistencia al repositorio. */
class AgendaViewModel(
    private val citaRepository: CitaRepository,
    private val servicioRepository: ServicioRepository,
    private val especialistaRepository: EspecialistaRepository,
    private val emailPaciente: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgendaUiState())
    val uiState: StateFlow<AgendaUiState> = _uiState.asStateFlow()
    private var consultaHorarios: Job? = null

    init {
        cargarEspecialistas()
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

    fun cargarEspecialistas() {
        _uiState.update { it.copy(cargandoEspecialistas = true, errorEspecialistas = null) }
        viewModelScope.launch {
            especialistaRepository.observarEspecialistas()
                .catch {
                    _uiState.update {
                        it.copy(cargandoEspecialistas = false, errorEspecialistas = "No se pudieron cargar los especialistas")
                    }
                }
                .collect { especialistas ->
                    _uiState.update { actual ->
                        // Una reserva nueva no puede conservar un profesional que dejó de estar activo.
                        val seleccionValida = actual.citaEditandoId != null ||
                            especialistas.any { it.id == actual.especialistaId }
                        actual.copy(
                            especialistas = especialistas,
                            especialistaId = actual.especialistaId.takeIf { seleccionValida },
                            cargandoEspecialistas = false,
                            errorEspecialistas = null
                        )
                    }
                }
        }
    }

    // Carga los datos de la base de datos para mostrarlos en el formulario
    fun prepararEdicion(citaId: Long) {
        viewModelScope.launch {
            val citaDetalle = citaRepository.obtenerDetallePorId(citaId) ?: return@launch
            val especialista = especialistaRepository.obtenerPorId(citaDetalle.cita.especialistaId)
            _uiState.update {
                it.copy(
                    citaEditandoId = citaId,
                    servicioSeleccionado = citaDetalle.servicio,
                    especialistaId = citaDetalle.cita.especialistaId,
                    especialistaDeCita = especialista,
                    fechaHoraInicio = citaDetalle.cita.fechaHoraInicio,
                    diaSeleccionado = HorariosAgenda.inicioDia(citaDetalle.cita.fechaHoraInicio),
                    modalidad = citaDetalle.cita.modalidad,
                    notas = citaDetalle.cita.notas ?: "",
                    errores = ErroresFormularioAgenda()
                )
            }
            cargarHorarios()
        }
    }

    fun onServicioSeleccionado(servicioId: Long) {
        viewModelScope.launch {
            val servicio = servicioRepository.obtenerPorId(servicioId)
            _uiState.update {
                it.copy(
                    servicioSeleccionado = servicio,
                    fechaHoraInicio = null,
                    modalidad = if (servicio?.permiteOnline == false) Modalidad.PRESENCIAL else it.modalidad,
                    errores = it.errores.copy(servicio = null, modalidad = null)
                )
            }
            cargarHorarios()
        }
    }

    fun onEspecialistaSeleccionado(especialistaId: Long) {
        val actual = _uiState.value
        if (actual.citaEditandoId != null || actual.operacion is EstadoOperacion.Cargando ||
            actual.especialistas.none { it.id == especialistaId }) return
        _uiState.update { it.copy(especialistaId = especialistaId, fechaHoraInicio = null,
            errores = it.errores.copy(especialista = null, fechaHora = null)) }
        cargarHorarios()
    }

    fun onDiaSeleccionado(dia: Long) {
        if (_uiState.value.operacion is EstadoOperacion.Cargando) return
        _uiState.update { it.copy(diaSeleccionado = HorariosAgenda.inicioDia(dia), fechaHoraInicio = null,
            errores = it.errores.copy(fechaHora = null)) }
        cargarHorarios()
    }

    fun onModalidadSeleccionada(modalidad: Modalidad) {
        val actual = _uiState.value
        if (actual.citaEditandoId != null || actual.operacion is EstadoOperacion.Cargando) return
        if (modalidad == Modalidad.ONLINE && actual.servicioSeleccionado?.permiteOnline != true) return
        _uiState.update { it.copy(modalidad = modalidad, errores = it.errores.copy(modalidad = null)) }
    }

    fun cargarHorarios() {
        consultaHorarios?.cancel()
        val actual = _uiState.value
        _uiState.update { it.copy(horariosDisponibles = emptyList(), errorHorarios = null, cargandoHorarios = false) }
        val dia = actual.diaSeleccionado ?: return
        val especialistaId = actual.especialistaId ?: return
        val servicio = actual.servicioSeleccionado ?: return
        _uiState.update { it.copy(cargandoHorarios = true) }
        consultaHorarios = viewModelScope.launch {
            citaRepository.observarHorariosDisponibles(dia, especialistaId, servicio.duracionMin, actual.citaEditandoId)
                .catch { _uiState.update { it.copy(cargandoHorarios = false, errorHorarios = "No se pudieron cargar los horarios") } }
                .collect { horarios ->
                    _uiState.update { it.copy(horariosDisponibles = horarios, cargandoHorarios = false,
                        fechaHoraInicio = it.fechaHoraInicio?.takeIf { fecha -> fecha in horarios }) }
                }
        }
    }

    fun onFechaHoraSeleccionada(epochMillis: Long) {
        if (_uiState.value.operacion is EstadoOperacion.Cargando ||
            epochMillis !in _uiState.value.horariosDisponibles) return
        _uiState.update { it.copy(fechaHoraInicio = epochMillis, errores = it.errores.copy(fechaHora = null)) }
    }

    fun confirmarAgendamiento() {
        val actual = _uiState.value
        if (actual.operacion is EstadoOperacion.Cargando) return
        if (actual.citaEditandoId == null &&
            (actual.cargandoEspecialistas || actual.errorEspecialistas != null)) return

        val servicioId = actual.servicioSeleccionado?.id
        val especialistaId = actual.especialistaId
        val fechaHora = actual.fechaHoraInicio

        val errores = ValidacionesAgenda.validar(servicioId, especialistaId, fechaHora)
        if (errores.hayErrores || servicioId == null || especialistaId == null || fechaHora == null) {
            _uiState.update { it.copy(errores = errores) }
            return
        }

        if (actual.cargandoHorarios || fechaHora !in actual.horariosDisponibles) {
            _uiState.update { it.copy(errores = it.errores.copy(fechaHora = "Selecciona un horario disponible")) }
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
            // Una reserva nueva inserta una cita; la reprogramación actualiza la fecha de la cita existente.
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
                        diaSeleccionado = null, horariosDisponibles = emptyList(), especialistaDeCita = null,
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
        ErrorAgenda.FechaPasada, ErrorAgenda.HorarioOcupado, ErrorAgenda.HorarioFueraAtencion,
        ErrorAgenda.AnticipacionInsuficiente, ErrorAgenda.CambioFueraPlazo -> base.copy(fechaHora = error.mensaje)
        ErrorAgenda.ServicioNoDisponible -> base.copy(servicio = error.mensaje)
        ErrorAgenda.EspecialistaNoDisponible -> base.copy(especialista = error.mensaje)
        ErrorAgenda.ModalidadNoDisponible -> base.copy(modalidad = error.mensaje)
        else -> base
    }
}
