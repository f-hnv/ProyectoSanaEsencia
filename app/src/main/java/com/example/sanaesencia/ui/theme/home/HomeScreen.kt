package com.example.sanaesencia.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCatalogo: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    onNavigateToPerfil: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola, Usuario", color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Tu próxima sesión",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Terapia Psicológica",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Mañana a las 15:00 hrs")
                }
            }

            Text(
                text = "¿Qué necesitas hacer hoy?",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MenuButton(
                    modifier = Modifier.weight(1f),
                    titulo = "Catálogo",
                    icono = Icons.Filled.List,
                    onClick = onNavigateToCatalogo
                )
                MenuButton(
                    modifier = Modifier.weight(1f),
                    titulo = "Agenda",
                    icono = Icons.Filled.CalendarMonth,
                    onClick = onNavigateToAgenda
                )
            }

            MenuButton(
                modifier = Modifier.fillMaxWidth(),
                titulo = "Mi Perfil",
                icono = Icons.Filled.Person,
                onClick = onNavigateToPerfil
            )
        }
    }
}

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    titulo: String,
    icono: ImageVector,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
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
        HomeScreen(
            onNavigateToCatalogo = {},
            onNavigateToAgenda = {},
            onNavigateToPerfil = {}
        )
    }
}
