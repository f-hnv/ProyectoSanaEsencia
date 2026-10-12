package com.example.sanaesencia.ui.agenda

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.viewmodel.AgendaViewModel
import com.example.sanaesencia.viewmodel.EstadoOperacion
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    servicioIdPreseleccionado: Long? = null,
    citaIdAEditar: Long? = null,
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
            viewModel.onEspecialistaSeleccionado(1L)
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

    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            viewModel.onFechaHoraSeleccionada(calendar.timeInMillis)
        },
        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            calendar.set(year, month, day)
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val esEdicion = estado.citaEditandoId != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (esEdicion) "Reprogramar Sesión" else "Agendar Sesión") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
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
                    Text("Servicio Seleccionado", style = MaterialTheme.typography.labelMedium)
                    Text(
                        estado.servicioSeleccionado?.nombre ?: "Selecciona un servicio en el catálogo",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                    )
                }
            }

            val fechaFormateada = remember(estado.fechaHoraInicio) {
                estado.fechaHoraInicio?.let {
                    val cal = Calendar.getInstance().apply { timeInMillis = it }
                    DateFormat.format("dd/MM/yyyy HH:mm", cal).toString()
                } ?: "Toca para elegir fecha y hora"
            }

            OutlinedTextField(
                value = fechaFormateada,
                onValueChange = { },
                readOnly = true, enabled = false,
                label = { Text("Fecha y Hora") },
                isError = estado.errores.fechaHora != null,
                supportingText = { estado.errores.fechaHora?.let { Text(it) } },
                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = "Calendario") },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.confirmarAgendamiento() },
                enabled = estado.operacion !is EstadoOperacion.Cargando,
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AgendaScreenPreview() {
    SanaesenciaTheme {
        AgendaScreen(onNavigateBack = {})
    }
}