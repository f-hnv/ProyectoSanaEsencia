package com.example.sanaesencia.ui.catalogo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.data.local.entity.ServicioEntity
import com.example.sanaesencia.ui.theme.SanaesenciaTheme

// Importaciones de tu backend (Ajusta estas 3 líneas si tus carpetas se llaman distinto)
import com.example.sanaesencia.viewmodel.CatalogoViewModel
import com.example.sanaesencia.viewmodel.ViewModelFactory

// 1. PANTALLA PRINCIPAL CONECTADA (Cerebro)
@Composable
fun CatalogoScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAgenda: (Long) -> Unit
) {
    // 1. Obtenemos el contenedor de tu aplicación
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.example.sanaesencia.SanaesenciaApp
    val factory = com.example.sanaesencia.viewmodel.ViewModelFactory(app.container)

    // 2. Inyectamos el ViewModel usando tu fábrica
    val viewModel: CatalogoViewModel = viewModel(factory = factory)

    // 3. Recolectamos la lista de la base de datos de Room en tiempo real
    val estado by viewModel.uiState.collectAsState()
    val listaServicios = estado.servicios

    // Pasamos los datos reales al diseño visual
    CatalogoContent(
        listaServicios = listaServicios,
        onNavigateBack = onNavigateBack,
        onNavigateToAgenda = onNavigateToAgenda
    )
}

// 2. DISEÑO VISUAL (Maqueta de Antonia)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoContent(
    listaServicios: List<ServicioEntity>,
    onNavigateBack: () -> Unit,
    onNavigateToAgenda: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuestros Servicios") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(listaServicios) { servicio ->
                Card(
                    onClick = { onNavigateToAgenda(servicio.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = servicio.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = servicio.descripcion,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            // Asumiendo que precio es Int en tu entidad. Si es String, quita el "$" extra
                            text = "$${servicio.precioClp}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// 3. PREVIEW (Mantiene datos falsos solo para previsualizar el diseño en Android Studio)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CatalogoScreenPreview() {
    SanaesenciaTheme {
        CatalogoContent(
            listaServicios = listOf(
                ServicioEntity(id = 1, nombre = "Terapia Psicológica", descripcion = "Sesión individual.", precioClp = 35000, categoria = "Terapia", duracionMin = 60),
                ServicioEntity(id = 2, nombre = "Masoterapia", descripcion = "Masaje descontracturante.", precioClp = 25000, categoria = "Terapia", duracionMin = 60)
            ),
            onNavigateBack = {},
            onNavigateToAgenda = {}
        )
    }
}
