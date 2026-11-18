package com.example.sanaesencia.ui.mensajes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.viewmodel.MensajesViewModel
import com.example.sanaesencia.domain.model.HorariosAgenda

@Composable
fun MensajesScreen(vm:MensajesViewModel=viewModel()) {
    val teclado=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val foco=androidx.compose.ui.platform.LocalFocusManager.current
    val lista=rememberLazyListState()
    LaunchedEffect(vm.mensajes.size){ if(vm.mensajes.isNotEmpty()) lista.animateScrollToItem(vm.mensajes.lastIndex) }
    Column(Modifier.fillMaxSize().imePadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Mensajes con el centro",style=MaterialTheme.typography.headlineSmall)
        Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)) {
            Text("Demostración local: los mensajes se guardan solo en este dispositivo. No se envían al centro real ni a otro teléfono.",Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)
        }
        if(vm.usuario?.rol!="PACIENTE") {
            Text("Selecciona una conversación",style=MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(vm.pacientes,key={it.email}) { p -> FilterChip(selected=vm.seleccionado==p.email,
                    onClick={vm.seleccionar(p.email)},label={Text(p.nombre)}) }
            }
            if(vm.pacientes.isEmpty()) Text("Todavía no hay pacientes registrados.")
        }
        if(vm.seleccionado==null) Text("Elige un paciente para consultar y responder sus mensajes.")
        LazyColumn(state=lista,modifier=Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(vm.mensajes.isEmpty() && vm.seleccionado!=null) item { Text("No hay mensajes todavía. Puedes iniciar la conversación.") }
            items(vm.mensajes,key={it.id}) { m ->
                val propio=m.remitenteEmail==vm.usuario?.email
                Column(Modifier.fillMaxWidth(),horizontalAlignment=if(propio) Alignment.End else Alignment.Start) {
                    Card(Modifier.widthIn(max=300.dp),colors=CardDefaults.cardColors(containerColor=if(propio)
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(m.remitenteNombre,style=MaterialTheme.typography.labelMedium)
                            Text(m.texto)
                            Text(HorariosAgenda.formatear(m.creadoEn,"dd/MM HH:mm"),style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        vm.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        if(vm.seleccionado!=null) {
            OutlinedTextField(vm.texto,vm::escribir,label={Text("Escribe un mensaje")},modifier=Modifier.fillMaxWidth(),maxLines=4)
            Button(onClick={teclado?.hide();foco.clearFocus();vm.enviar()},enabled=vm.texto.isNotBlank() && !vm.enviando,modifier=Modifier.fillMaxWidth()) {
                Text(if(vm.enviando) "Guardando…" else "Guardar mensaje local")
            }
        }
    }
}
