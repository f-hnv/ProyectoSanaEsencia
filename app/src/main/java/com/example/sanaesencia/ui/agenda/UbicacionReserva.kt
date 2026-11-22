package com.example.sanaesencia.ui.agenda

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.domain.model.*
import com.example.sanaesencia.viewmodel.*

/** Consulta GPS en la propia reserva: no es necesario pasar primero por «Cómo llegar». */
@Composable
fun UbicacionReserva(factory: ViewModelFactory, permiteOnline: Boolean, modalidad: Modalidad,
    enabled: Boolean, onElegirOnline: () -> Unit,
    onUbicacionCambio: (EstadoUbicacion) -> Unit = {}) {
    val vm: SedeViewModel = viewModel(key = "ubicacion-reserva", factory = factory)
    val estado by vm.uiState.collectAsState()
    LaunchedEffect(estado.estado) { onUbicacionCambio(estado.estado) }
    val umbral by ConfiguracionCentro.umbralKm.collectAsState()
    val permisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onPermisoResultado(vm.tienePermiso())
    }
    LaunchedEffect(vm) { if (vm.tienePermiso()) vm.actualizarUbicacion() }
    UbicacionReservaContent(estado, umbral, permiteOnline, modalidad, enabled,
        onConsultar = {
            if (vm.tienePermiso()) vm.actualizarUbicacion() else permisos.launch(vm.permisosRequeridos)
        }, onElegirOnline = onElegirOnline)
}

@Composable
fun UbicacionReservaContent(estado: SedeUiState, umbralKm: Float, permiteOnline: Boolean,
    modalidad: Modalidad, enabled: Boolean, onConsultar: () -> Unit, onElegirOnline: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ubicación y modalidad", style = MaterialTheme.typography.titleMedium)
            when (estado.estado) {
                EstadoUbicacion.Inicial -> Text("Consulta tu ubicación para saber si te conviene una sesión online. También puedes elegir la modalidad manualmente.")
                EstadoUbicacion.Cargando -> {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text("Calculando tu distancia al centro…")
                }
                EstadoUbicacion.PermisoDenegado -> Text("No se concedió ubicación. Puedes reservar y elegir tu modalidad igualmente.")
                EstadoUbicacion.UbicacionDesactivada -> Text("Activa el GPS del teléfono y vuelve a consultar. La reserva sigue disponible.")
                EstadoUbicacion.NoDisponible -> Text("No se pudo obtener una ubicación reciente. Reintenta o elige tu modalidad manualmente.")
                is EstadoUbicacion.Exito -> {
                    Text("Estás a ${estado.distanciaTexto ?: "una distancia desconocida"} del centro.")
                    Text("Distancia aproximada en línea recta.", style = MaterialTheme.typography.bodySmall)
                    val lejos = RecomendacionModalidad.sugerirOnline(estado.distanciaMetros, umbralKm, true)
                    when {
                        lejos && permiteOnline -> {
                            Text("Estás a más de ${umbralKm.toInt()} km. Te sugerimos una sesión online.",
                                color = MaterialTheme.colorScheme.primary)
                            if (modalidad == Modalidad.ONLINE) Text("Seleccionaste atención online · sin box")
                            else Button(onClick = onElegirOnline, enabled = enabled) { Text("Elegir sesión online") }
                        }
                        lejos -> Text("Estás a más de ${umbralKm.toInt()} km, pero este servicio solo admite atención presencial.")
                        else -> Text("Puedes elegir atención presencial${if (permiteOnline) " u online" else ""}.")
                    }
                    Text("La sugerencia no impide reservar desde otra ciudad o país.", style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = onConsultar, enabled = enabled && estado.estado != EstadoUbicacion.Cargando) {
                Text(if (estado.estado is EstadoUbicacion.Exito) "Actualizar mi ubicación" else "Usar mi ubicación")
            }
        }
    }
}
