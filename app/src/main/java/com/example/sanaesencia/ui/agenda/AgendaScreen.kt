package com.example.sanaesencia.ui.agenda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import com.example.sanaesencia.viewmodel.AgendaViewModel
import com.example.sanaesencia.viewmodel.EstadoOperacion
import java.util.Calendar
import com.example.sanaesencia.domain.model.HorariosAgenda
import com.example.sanaesencia.domain.model.Modalidad
import java.text.NumberFormat
import java.util.Locale
import com.example.sanaesencia.ui.components.SanaPrimaryButton as Button
import com.example.sanaesencia.ui.components.SanaChoiceChip as FilterChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    servicioIdPreseleccionado: Long? = null,
    citaIdAEditar: Long? = null,
    onNavigateToSede: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as com.example.sanaesencia.SanaesenciaApp
    val factory = com.example.sanaesencia.viewmodel.ViewModelFactory(app.container)
    val viewModel: AgendaViewModel = viewModel(factory = factory)

    val estado by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(servicioIdPreseleccionado, citaIdAEditar) {
        if (citaIdAEditar != null) {
            viewModel.prepararEdicion(citaIdAEditar)
        } else {
            if (servicioIdPreseleccionado != null) viewModel.onServicioSeleccionado(servicioIdPreseleccionado)
        }
    }

    LaunchedEffect(estado.operacion) {
        when (val op = estado.operacion) {
            is EstadoOperacion.Exito -> {
                snackbarHostState.showSnackbar(op.mensaje)
                viewModel.resultadoConsumido()
                onNavigateBack()
            }
            is EstadoOperacion.Error -> {
                snackbarHostState.showSnackbar(op.error.mensaje)
                viewModel.resultadoConsumido()
            }
            else -> {}
        }
    }

    val esEdicion = citaIdAEditar != null
    val cargandoOperacion = estado.operacion is EstadoOperacion.Cargando

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (esEdicion) "Reprogramar Sesión" else "Agendar Sesión") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (esEdicion) "Modifica tu fecha y hora" else "Reserva tu cita",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tu servicio", style = MaterialTheme.typography.labelMedium)
                    Text(
                        estado.servicioSeleccionado?.nombre ?: "Selecciona un servicio en el catálogo",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                    )
                }
            }

            if (esEdicion) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Especialista de tu cita", style = MaterialTheme.typography.labelLarge)
                        Text(estado.especialistaDeCita?.nombre ?: "Cargando especialista…",
                            style = MaterialTheme.typography.titleMedium)
                        estado.especialistaDeCita?.let { Text(it.especialidad) }
                        Text("Al reprogramar se mantiene el profesional y solo cambia la fecha y hora.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                when {
                    estado.cargandoEspecialistas -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Cargando especialistas…")
                    }
                    estado.errorEspecialistas != null -> {
                        Text(estado.errorEspecialistas.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::cargarEspecialistas) { Text("Reintentar") }
                    }
                    else -> SelectorEspecialistas(
                        especialistas = estado.especialistas,
                        seleccionadoId = estado.especialistaId,
                        error = estado.errores.especialista,
                        enabled = !cargandoOperacion,
                        onSeleccionar = viewModel::onEspecialistaSeleccionado
                    )
                }
            }

            if (!esEdicion && estado.servicioSeleccionado != null) {
                Text("Modalidad de atención", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = estado.modalidad == Modalidad.PRESENCIAL,
                        onClick = { viewModel.onModalidadSeleccionada(Modalidad.PRESENCIAL) },
                        enabled = !cargandoOperacion, label = { Text("Presencial") },
                        modifier = Modifier.heightIn(min = 48.dp))
                    if (estado.servicioSeleccionado?.permiteOnline == true) {
                        FilterChip(selected = estado.modalidad == Modalidad.ONLINE,
                            onClick = { viewModel.onModalidadSeleccionada(Modalidad.ONLINE) },
                            enabled = !cargandoOperacion, label = { Text("Online") },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
            }

            Text("Elige un día", style = MaterialTheme.typography.titleMedium)
            Text(HorariosAgenda.DESCRIPCION, style = MaterialTheme.typography.bodySmall)
            SelectorDias(estado.diaSeleccionado, estado.servicioSeleccionado?.duracionMin ?: 50,
                enabled = !cargandoOperacion, onSeleccionar = viewModel::onDiaSeleccionado)
            Text("Toca una hora disponible", style = MaterialTheme.typography.titleMedium)
            Text("No necesitas escribir la hora. Solo elige uno de los botones.", style = MaterialTheme.typography.bodySmall)
            when {
                estado.servicioSeleccionado == null -> Text("Primero selecciona un servicio desde el catálogo.")
                estado.especialistaId == null -> Text("Elige un especialista para ver sus horarios.")
                estado.diaSeleccionado == null -> Text("Selecciona un día para consultar disponibilidad.")
                estado.cargandoHorarios -> CircularProgressIndicator(Modifier.size(24.dp))
                estado.errorHorarios != null -> {
                    Text(estado.errorHorarios.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::cargarHorarios) { Text("Reintentar horarios") }
                }
                estado.horariosDisponibles.isEmpty() -> Text("No hay horas disponibles para este día. Prueba otra fecha.")
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Dos columnas para mantener botones cómodos en pantallas pequeñas.
                    estado.horariosDisponibles.chunked(2).forEach { fila ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            fila.forEach { hora ->
                                FilterChip(selected = estado.fechaHoraInicio == hora,
                                    onClick = { viewModel.onFechaHoraSeleccionada(hora) },
                                    enabled = !cargandoOperacion,
                                    label = { Text(HorariosAgenda.formatear(hora, "HH:mm"), style = MaterialTheme.typography.titleMedium) },
                                    leadingIcon = if (estado.fechaHoraInicio == hora) {
                                        { Icon(Icons.Filled.CheckCircle, contentDescription = "Seleccionada") }
                                    } else null,
                                    modifier = Modifier.weight(1f).heightIn(min = 56.dp))
                            }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            estado.errores.fechaHora?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            AnimatedVisibility(visible = estado.fechaHoraInicio != null) {
                Card(modifier = Modifier.fillMaxWidth().animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Revisa tu reserva", style = MaterialTheme.typography.titleMedium)
                        Text(estado.servicioSeleccionado?.nombre.orEmpty(), fontWeight = FontWeight.SemiBold)
                        Text((estado.especialistaDeCita ?: estado.especialistas.find { it.id == estado.especialistaId })?.nombre.orEmpty())
                        estado.fechaHoraInicio?.let { Text(HorariosAgenda.formatear(it, "EEEE dd 'de' MMMM · HH:mm")) }
                        Text(if (estado.modalidad == Modalidad.ONLINE) "Atención online" else "Presencial · Caupolicán 220, Los Vilos")
                        estado.servicioSeleccionado?.let { servicio ->
                            val precio = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL")).apply { maximumFractionDigits = 0 }
                            Text("${servicio.duracionMin} minutos · ${precio.format(servicio.precioClp)}")
                        }
                        if (estado.modalidad == Modalidad.PRESENCIAL) {
                            TextButton(onClick = onNavigateToSede) { Text("Ver cómo llegar") }
                        }
                        Text("Puedes cancelar o reprogramar con al menos 12 horas de anticipación.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Button(
                onClick = { viewModel.confirmarAgendamiento() },
                enabled = !cargandoOperacion && !estado.cargandoHorarios && estado.errorHorarios == null &&
                    estado.fechaHoraInicio != null && estado.fechaHoraInicio in estado.horariosDisponibles &&
                    estado.servicioSeleccionado != null && estado.especialistaId != null &&
                    (esEdicion || (!estado.cargandoEspecialistas && estado.errorEspecialistas == null)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (estado.operacion is EstadoOperacion.Cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (esEdicion) "Guardar Cambios" else "Confirmar Reserva")
                }
            }
        }
    }
}

@Composable
private fun SelectorDias(seleccionado: Long?, duracionMin: Int, enabled: Boolean, onSeleccionar: (Long) -> Unit) {
    var pagina by rememberSaveable { mutableIntStateOf(0) }
    val hoy = HorariosAgenda.inicioDia(System.currentTimeMillis())
    val inicio = HorariosAgenda.calendario(hoy).apply { add(Calendar.DAY_OF_YEAR, pagina * 7) }.timeInMillis
    val dias = HorariosAgenda.diasParaElegir(inicio, duracionMin, System.currentTimeMillis())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { pagina-- }, enabled = enabled && pagina > 0) { Text("Anterior") }
            TextButton(onClick = { pagina++ }, enabled = enabled) { Text("Más fechas") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dias, key = { it }) { dia ->
                FilterChip(selected = seleccionado == dia, onClick = { onSeleccionar(dia) }, enabled = enabled,
                    label = { Text(HorariosAgenda.formatear(dia, "EEE dd MMM")) },
                    modifier = Modifier.heightIn(min = 56.dp).testTag("dia-$dia"))
            }
        }
        seleccionado?.let { Text("Día elegido: ${HorariosAgenda.formatear(it, "EEEE dd/MM/yyyy")}",
            style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun SelectorEspecialistas(
    especialistas: List<EspecialistaEntity>,
    seleccionadoId: Long?,
    error: String?,
    enabled: Boolean,
    onSeleccionar: (Long) -> Unit
) {
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Elige tu especialista", style = MaterialTheme.typography.titleMedium)
        Text("Revisa su especialidad antes de confirmar tu reserva.", style = MaterialTheme.typography.bodyMedium)
        if (especialistas.isEmpty()) {
            Text("No hay especialistas disponibles para reservar en este momento.")
        }
        especialistas.forEach { especialista ->
            val seleccionado = especialista.id == seleccionadoId
            // Toda la tarjeta es una única opción accesible, incluido el indicador de selección.
            Card(
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = seleccionado,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = { onSeleccionar(especialista.id) }
                ),
                colors = CardDefaults.cardColors(containerColor = if (seleccionado)
                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                border = BorderStroke(if (seleccionado) 2.dp else 1.dp,
                    if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = .55f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected = seleccionado, onClick = null, enabled = enabled)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(especialista.nombre, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(especialista.especialidad, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary)
                        if (especialista.descripcion.isNotBlank()) {
                            Text(especialista.descripcion, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AgendaScreenPreview() {
    SanaesenciaTheme {
        Column(Modifier.padding(24.dp)) {
            SelectorEspecialistas(
                especialistas = listOf(
                    EspecialistaEntity(id = 1, nombre = "Profesional de ejemplo", especialidad = "Psicología clínica",
                        descripcion = "Perfil ficticio para vista previa.")
                ),
                seleccionadoId = 1L,
                error = null,
                enabled = true,
                onSeleccionar = {}
            )
        }
    }
}
