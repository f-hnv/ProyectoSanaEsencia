package com.example.sanaesencia.ui.home

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.viewmodel.AgendaViewModel
import com.example.sanaesencia.data.local.relation.CitaConDetalle
import java.util.Calendar

@Composable
fun HomeScreen(
    onNavigateToCatalogo: () -> Unit,
    onNavigateToAgenda: (Long?, Long?) -> Unit,
    onNavigateToPerfil: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as com.example.sanaesencia.SanaesenciaApp
    val factory = com.example.sanaesencia.viewmodel.ViewModelFactory(app.container)
    val viewModel: AgendaViewModel = viewModel(factory = factory)

    val estado by viewModel.uiState.collectAsState()
    val proximasCitas = estado.proximasCitas

    val sharedPref = context.getSharedPreferences("sanaesencia_prefs", Context.MODE_PRIVATE)
    val nombreUsuario = sharedPref.getString("nombre_registrado", "Usuario") ?: "Usuario"

    HomeContent(
        nombreUsuario = nombreUsuario,
        proximasCitas = proximasCitas,
        onCancelarCita = { citaId -> viewModel.cancelarCita(citaId) },
        onNavigateToCatalogo = onNavigateToCatalogo,
        onNavigateToAgenda = onNavigateToAgenda,
        onNavigateToPerfil = onNavigateToPerfil
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    nombreUsuario: String,
    proximasCitas: List<CitaConDetalle>,
    onCancelarCita: (Long) -> Unit,
    onNavigateToCatalogo: () -> Unit,
    onNavigateToAgenda: (Long?, Long?) -> Unit,
    onNavigateToPerfil: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola, $nombreUsuario", color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Text("Tus próximas sesiones", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

            if (proximasCitas.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("No tienes citas próximas", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("¡Anímate a agendar tu primera sesión!", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                proximasCitas.forEach { citaConDetalle ->
                    val fechaCita = citaConDetalle.cita.fechaHoraInicio
                    val citaId = citaConDetalle.cita.id
                    val nombreServicio = citaConDetalle.servicio.nombre

                    val cal = Calendar.getInstance().apply { timeInMillis = fechaCita }
                    val fechaStr = DateFormat.format("dd/MM/yyyy HH:mm", cal).toString()

                    val ahora = System.currentTimeMillis()
                    val diferenciaMilis = fechaCita - ahora
                    val doceHorasMilis = 12 * 60 * 60 * 1000L
                    val sePuedeCancelar = diferenciaMilis > doceHorasMilis

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(nombreServicio, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(fechaStr, style = MaterialTheme.typography.bodyMedium)

                            Spacer(modifier = Modifier.height(16.dp))

                            if (sePuedeCancelar) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onNavigateToAgenda(null, citaId) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Modificar") }

                                    OutlinedButton(
                                        onClick = { onCancelarCita(citaId) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Cancelar") }
                                }
                            } else {
                                Text(
                                    "Modificación no disponible (menos de 12 hrs para la sesión)",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            Text("¿Qué necesitas hacer hoy?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MenuButton(modifier = Modifier.weight(1f), titulo = "Catálogo", icono = Icons.Filled.List, onClick = onNavigateToCatalogo)
                MenuButton(modifier = Modifier.weight(1f), titulo = "Agenda", icono = Icons.Filled.CalendarMonth, onClick = { onNavigateToAgenda(null, null) })
            }

            MenuButton(modifier = Modifier.fillMaxWidth(), titulo = "Mi Perfil", icono = Icons.Filled.Person, onClick = onNavigateToPerfil)
        }
    }
}

@Composable
fun MenuButton(modifier: Modifier = Modifier, titulo: String, icono: ImageVector, onClick: () -> Unit) {
    ElevatedButton(onClick = onClick, modifier = modifier.height(80.dp), shape = RoundedCornerShape(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icono, contentDescription = titulo)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = titulo)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    SanaesenciaTheme {
        HomeContent(
            nombreUsuario = "Carlos Pérez",
            proximasCitas = emptyList(),
            onCancelarCita = {},
            onNavigateToCatalogo = {},
            onNavigateToAgenda = { _, _ -> },
            onNavigateToPerfil = {}
        )
    }
}