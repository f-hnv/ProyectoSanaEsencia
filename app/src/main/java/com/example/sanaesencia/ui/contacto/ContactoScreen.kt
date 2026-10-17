package com.example.sanaesencia.ui.contacto

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sanaesencia.domain.model.ContactoSanaesencia
import com.example.sanaesencia.domain.model.SedeSanaesencia
import com.example.sanaesencia.ui.components.SanaPrimaryButton as Button
import com.example.sanaesencia.ui.components.SanaOutlineButton as OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactoScreen(onNavigateBack: () -> Unit, onNavigateToSede: () -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Contáctanos") }, navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Estamos para orientarte", style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary)
            Text("Consulta por nuestros servicios, modalidades de atención y coordinación de sesiones.")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("WhatsApp del centro", style = MaterialTheme.typography.titleMedium)
                    Text(ContactoSanaesencia.TELEFONO_VISIBLE, style = MaterialTheme.typography.titleLarge)
                    Text("Se abrirá una consulta preparada. Puedes editarla antes de enviarla.")
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        error = null
                        val uri = Uri.Builder().scheme("https").authority("wa.me")
                            .appendPath(ContactoSanaesencia.WHATSAPP_NUMERO)
                            .appendQueryParameter("text", ContactoSanaesencia.MENSAJE).build()
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp"))
                        } catch (_: ActivityNotFoundException) {
                            try {
                                // También permite usar WhatsApp Business o continuar desde el navegador.
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (_: ActivityNotFoundException) {
                                error = "No se pudo abrir WhatsApp ni un navegador. Puedes contactarnos al número que aparece arriba."
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Escribir por WhatsApp")
                    }
                }
            }
            Text(SedeSanaesencia.NOMBRE, style = MaterialTheme.typography.titleMedium)
            Text(SedeSanaesencia.DIRECCION)
            OutlinedButton(onClick = onNavigateToSede, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cómo llegar")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
