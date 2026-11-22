package com.example.sanaesencia.ui.terapeuta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.domain.model.*
import com.example.sanaesencia.viewmodel.*

@Composable
fun TerapeutaScreen(vm: TerapeutaViewModel? = null) {
    if(vm==null) { AgendaEspecialistaServidor(); return }
    val estado by vm.estado.collectAsState()
    TerapeutaContent(estado, vm::cambiarMes, vm::cargar)
}

@Composable
fun TerapeutaContent(estado: AgendaTerapeutaState, onCambiarMes: (Int) -> Unit, onReintentar: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("agenda-terapeuta"),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Mi agenda mensual", style = MaterialTheme.typography.headlineSmall)
            Text(estado.usuario?.nombre.orEmpty(), style = MaterialTheme.typography.titleMedium)
            Text("Citas de tu perfil profesional · hora de Chile", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onCambiarMes(-1) }, modifier = Modifier.testTag("mes-anterior")) { Text("Mes anterior") }
                TextButton(onClick = { onCambiarMes(1) }, modifier = Modifier.testTag("mes-siguiente")) { Text("Mes siguiente") }
            }
            Text(estado.periodo.titulo, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag("mes-agenda"))
        }
        when {
            estado.cargando -> item { CircularProgressIndicator(); Text("Cargando citas del mes…") }
            estado.error != null -> item {
                Text(estado.error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onReintentar) { Text("Reintentar agenda") }
            }
            else -> {
                item {
                    val programadas = estado.citas.count { it.cita.estado == EstadoCita.PROGRAMADA }
                    Text("${estado.citas.size} citas en el mes · $programadas programadas")
                    if (estado.citas.isEmpty()) Text("No tienes citas agendadas para este mes.")
                }
                items(estado.citas, key = { it.cita.id }) { detalle ->
                    val cita = detalle.cita
                    Card(Modifier.fillMaxWidth().testTag("cita-terapeuta-${cita.id}")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(detalle.servicio.nombre, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(HorariosAgenda.formatear(cita.fechaHoraInicio, "EEEE dd/MM · HH:mm"))
                            Text("Paciente: ${cita.emailPaciente}")
                            Text("Duración: ${cita.duracionMin} minutos")
                            Text(if (cita.modalidad == Modalidad.ONLINE) "Online · sin box"
                                else cita.boxId?.let { "Presencial · Box $it" } ?: "Presencial · box pendiente de asignar")
                            Text("Estado: ${cita.estado.name.lowercase().replace('_', ' ')}")
                            if (cita.notas.isNotBlank()) Text("Observación de la reserva: ${cita.notas}")
                        }
                    }
                }
            }
        }
    }
}
