package com.example.sanaesencia.ui.sede

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.SanaesenciaApp
import com.example.sanaesencia.domain.model.SedeSanaesencia
import com.example.sanaesencia.viewmodel.*
import com.example.sanaesencia.ui.components.SanaPrimaryButton as Button
import com.example.sanaesencia.ui.components.SanaOutlineButton as OutlinedButton

@Composable
fun SedeScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SanaesenciaApp
    val vm: SedeViewModel = viewModel(factory = ViewModelFactory(app.container))
    val estado by vm.uiState.collectAsState()
    var errorMapa by remember { mutableStateOf<String?>(null) }
    val permisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onPermisoResultado(vm.tienePermiso())
    }
    val ajustes = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.actualizarUbicacion()
    }

    SedeContent(estado, onNavigateBack,
        onUbicar = {
            if (vm.tienePermiso()) vm.actualizarUbicacion() else permisos.launch(vm.permisosRequeridos)
        },
        onAjustes = {
            val intent = if (estado.estado is EstadoUbicacion.UbicacionDesactivada)
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            ajustes.launch(intent)
        },
        onMapa = {
            // Solo se entrega el destino; las coordenadas del paciente no se guardan ni se incluyen en la URL.
            val destino = "${SedeSanaesencia.LATITUD},${SedeSanaesencia.LONGITUD}"
            val mapa = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$destino?q=$destino(${Uri.encode(SedeSanaesencia.NOMBRE)})"))
            try {
                context.startActivity(mapa)
            } catch (_: ActivityNotFoundException) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$destino")))
                } catch (_: ActivityNotFoundException) {
                    errorMapa = "No hay una aplicación de mapas o navegador disponible."
                }
            }
        }, errorMapa = errorMapa)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SedeContent(
    estado: SedeUiState,
    onNavigateBack: () -> Unit,
    onUbicar: () -> Unit,
    onAjustes: () -> Unit,
    onMapa: () -> Unit,
    errorMapa: String? = null
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Cómo llegar") }, navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(SedeSanaesencia.NOMBRE, style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary)
            Text(SedeSanaesencia.DIRECCION, style = MaterialTheme.typography.titleMedium)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tu distancia al centro", style = MaterialTheme.typography.titleMedium)
                    Text("Usamos tu ubicación mientras consultas esta pantalla para calcular la distancia a la sede.")
                    when (val ubicacion = estado.estado) {
                        EstadoUbicacion.Inicial -> Text("Toca el botón para consultar tu ubicación.")
                        EstadoUbicacion.Cargando -> {
                            CircularProgressIndicator(Modifier.size(24.dp))
                            Text("Buscando tu ubicación…")
                        }
                        EstadoUbicacion.PermisoDenegado -> Text("No se concedió el permiso de ubicación. Puedes permitirlo en los ajustes de la aplicación o consultar la dirección en mapas.")
                        EstadoUbicacion.UbicacionDesactivada -> Text("La ubicación está desactivada. Actívala para calcular la distancia.")
                        EstadoUbicacion.NoDisponible -> Text("No pudimos obtener una ubicación reciente. Comprueba la señal y vuelve a intentarlo.")
                        is EstadoUbicacion.Exito -> {
                            Text("Estás a ${estado.distanciaTexto} de la sede", style = MaterialTheme.typography.titleLarge)
                            Text("Distancia aproximada en línea recta; el recorrido puede ser mayor.", style = MaterialTheme.typography.bodySmall)
                            ubicacion.precisionM?.let {
                                Text("Precisión aproximada: ${it.toInt()} m", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Button(onClick = onUbicar, enabled = estado.estado !is EstadoUbicacion.Cargando) {
                        Text(if (estado.estado is EstadoUbicacion.Exito) "Actualizar ubicación" else "Usar mi ubicación")
                    }
                    if (estado.estado is EstadoUbicacion.PermisoDenegado || estado.estado is EstadoUbicacion.UbicacionDesactivada) {
                        OutlinedButton(onClick = onAjustes) { Text("Abrir ajustes") }
                    }
                }
            }
            OutlinedButton(onClick = onMapa, modifier = Modifier.fillMaxWidth()) { Text("Ver sede en mapas") }
            errorMapa?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
