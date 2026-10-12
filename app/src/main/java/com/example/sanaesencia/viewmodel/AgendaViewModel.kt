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

/** Estado de la última operación (agendar o cancelar). */
sealed interface EstadoOperacion {
    data object Inactivo : EstadoOperacion                       // nada en curso
    data object Cargando : EstadoOperacion                       // → deshabilitar botón + mostrar progreso
    data class Exito(val mensaje: String, val citaId: Long? = null) : EstadoOperacion
    data class Error(val error: ErrorAgenda) : EstadoOperacion   // error.mensaje ya está en español
}

/**
 * Estado completo de agenda: formulario + listas de citas.
 *
 * FORMULARIO
 * @property servicioSeleccionado servicio elegido (de aquí salen duración, precio y permiteOnline).
 * @property especialistaId       id del especialista elegido, null si no hay.
 * @property fechaHoraInicio      inicio en epoch millis (zona local), null si no hay.
 * @property modalidad            PRESENCIAL por defecto.
 * @property notas                texto libre opcional.
 * @property errores              un mensaje por campo; null = campo correcto → pintar en rojo + ícono.
 *
 * OPERACIÓN
 * @property operacion            Inactivo / Cargando / Exito / Error (ver EstadoOperacion).
 *
 * LISTAS (se actualizan solas)
 * @property proximasCitas        citas PROGRAMADAS futuras del paciente → Home / Reservas.
 * @property todasLasCitas        historial completo (todos los estados) → pestaña Reservas.
 */
data class AgendaUiState(
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

/**
 * ═══ CONTRATO PARA LA UI (Compose) ═══
 *
 * OBSERVAR (un único StateFlow):
 *   val estado by agendaVm.uiState.collectAsStateWithLifecycle()
 *
 *   // Campos del formulario
 *   OutlinedTextField(..., isError = estado.errores.fechaHora != null,
 *                     supportingText = { estado.errores.fechaHora?.let { Text(it) } })
 *
 *   // Reaccionar a la operación (una sola vez)
 *   LaunchedEffect(estado.operacion) {
 *       when (val op = estado.operacion) {
 *           is EstadoOperacion.Exito -> { /* snackbar(op.mensaje) / navegar */ agendaVm.resultadoConsumido() }
 *           is EstadoOperacion.Error -> { /* snackbar(op.error.mensaje) */ agendaVm.resultadoConsumido() }
 *           else -> Unit
 *       }
 *   }
 *   Button(enabled = estado.operacion !is EstadoOperacion.Cargando, ...)
 *
 * EVENTOS (funciones que la UI llama):
 *   onServicioSeleccionado(servicioId: Long)
 *   onEspecialistaSeleccionado(especialistaId: Long)
 *   onFechaHoraSeleccionada(epochMillis: Long)   // la UI combina DatePicker + TimePicker en millis locales
 *   onModalidadSeleccionada(modalidad: Modalidad)
 *   onNotasCambiadas(texto: String)
 *   confirmarAgendamiento()                      // valida → agenda → actualiza `operacion`
 *   cancelarCita(citaId: Long)
 *   resultadoConsumido()                         // la UI avisa que ya mostró Exito/Error → vuelve a Inactivo
 *
 * Los errores de campo se limpian solos cuando el usuario corrige ese campo.
 */
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

    // ── Eventos de formulario ────────────────────────────────────────────────

    fun onServicioSeleccionado(servicioId: Long) {
        viewModelScope.launch {
            val servicio = servicioRepository.obtenerPorId(servicioId)
            _uiState.update {
                it.copy(
                    servicioSeleccionado = servicio,
                    // Si el servicio no admite online y estaba elegido ONLINE, se corrige solo.
                    modalidad = if (servicio?.permiteOnline == false) Modalidad.PRESENCIAL else it.modalidad,
                    errores = it.errores.copy(servicio = null, modalidad = null)
                )
            }
        }
    }

    fun onEspecialistaSeleccionado(especialistaId: Long) {
        _uiState.update {
            it.copy(especialistaId = especialistaId, errores = it.errores.copy(especialista = null))
        }
    }

    fun onFechaHoraSeleccionada(epochMillis: Long) {
        _uiState.update {
            it.copy(fechaHoraInicio = epochMillis, errores = it.errores.copy(fechaHora = null))
        }
    }

    fun onModalidadSeleccionada(modalidad: Modalidad) {
        _uiState.update {
            if (modalidad == Modalidad.ONLINE && it.servicioSeleccionado?.permiteOnline == false) {
                it.copy(errores = it.errores.copy(modalidad = ErrorAgenda.ModalidadNoDisponible.mensaje))
            } else {
                it.copy(modalidad = modalidad, errores = it.errores.copy(modalidad = null))
            }
        }
    }

    fun onNotasCambiadas(texto: String) {
        _uiState.update { it.copy(notas = texto) }
    }

    // ── Acciones ─────────────────────────────────────────────────────────────

    fun confirmarAgendamiento() {
        val actual = _uiState.value
        if (actual.operacion is EstadoOperacion.Cargando) return // evita doble envío

        val servicioId = actual.servicioSeleccionado?.id
        val especialistaId = actual.especialistaId
        val fechaHora = actual.fechaHoraInicio

        val errores = ValidacionesAgenda.validar(servicioId, especialistaId, fechaHora)
        if (errores.hayErrores || servicioId == null || especialistaId == null || fechaHora == null) {
            _uiState.update { it.copy(errores = errores) }
            return
        }

        _uiState.update { it.copy(errores = ErroresFormularioAgenda(), operacion = EstadoOperacion.Cargando) }

        viewModelScope.launch {
            val resultado = citaRepository.agendar(
                emailPaciente = emailPaciente,
                servicioId = servicioId,
                especialistaId = especialistaId,
                fechaHoraInicio = fechaHora,
                modalidad = actual.modalidad,
                notas = actual.notas.trim()
            )
            when (resultado) {
                is Resultado.Exito -> _uiState.update {
                    // Éxito: se limpia el formulario para un nuevo agendamiento.
                    it.copy(
                        operacion = EstadoOperacion.Exito("Cita agendada correctamente", resultado.datos),
                        servicioSeleccionado = null,
                        especialistaId = null,
                        fechaHoraInicio = null,
                        modalidad = Modalidad.PRESENCIAL,
                        notas = ""
                    )
                }
                is Resultado.Error -> _uiState.update {
                    it.copy(
                        operacion = EstadoOperacion.Error(resultado.error),
                        errores = erroresDeCampo(resultado.error, it.errores)
                    )
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
            // Las listas de citas se refrescan solas porque Room emite el cambio por Flow.
        }
    }

    fun resultadoConsumido() {
        _uiState.update { it.copy(operacion = EstadoOperacion.Inactivo) }
    }

    // Traduce un error de negocio al campo del formulario que debe marcarse en rojo.
    private fun erroresDeCampo(error: ErrorAgenda, base: ErroresFormularioAgenda) = when (error) {
        ErrorAgenda.FechaPasada, ErrorAgenda.HorarioOcupado -> base.copy(fechaHora = error.mensaje)
        ErrorAgenda.ServicioNoDisponible -> base.copy(servicio = error.mensaje)
        ErrorAgenda.EspecialistaNoDisponible -> base.copy(especialista = error.mensaje)
        ErrorAgenda.ModalidadNoDisponible -> base.copy(modalidad = error.mensaje)
        else -> base
    }
}

