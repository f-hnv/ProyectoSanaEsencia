package com.example.sanaesencia.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.SanaesenciaApp
import com.example.sanaesencia.data.remote.SesionBackend
import com.example.sanaesencia.ui.agenda.ReservasMes
import com.example.sanaesencia.utils.FechasReservas
import com.example.sanaesencia.viewmodel.*

@Composable
fun InicioServidor(onCatalogo:()->Unit,onAgenda:(Long?,Long?)->Unit,onPerfil:()->Unit,onSede:()->Unit,onContacto:()->Unit) {
    val app=LocalContext.current.applicationContext as SanaesenciaApp
    val usuario=SesionBackend.actual()?.usuario
    val vm:ListaReservasViewModel=viewModel(factory=ViewModelFactory(app.container))
    val s by vm.estado.collectAsState()
    val historico by remember(usuario?.id) {app.container.database.citaDao().observarPorPaciente(usuario?.correo.orEmpty())}.collectAsState(initial=emptyList())
    var verHistorico by remember {mutableStateOf(false)}
    LaunchedEffect(Unit){vm.cargar()}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Hola, ${usuario?.nombre.orEmpty()} ${usuario?.apellido.orEmpty()}",style=MaterialTheme.typography.headlineSmall)
        Button(onClick=onCatalogo,modifier=Modifier.fillMaxWidth()){Text("Reservar una sesión")}
        Text("Tus próximas sesiones",style=MaterialTheme.typography.titleLarge)
        Text("Citas del servidor · hora de Chile",style=MaterialTheme.typography.bodySmall)
        ReservasMes(s,"PACIENTE",vm::mes,vm::cargar,onEditar={onAgenda(null,it)},onEstado=vm::cambiarEstado)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=onCatalogo){Text("Catálogo")};OutlinedButton(onClick={onAgenda(null,null)}){Text("Agenda")}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=onPerfil){Text("Mi Perfil")};OutlinedButton(onClick=onSede){Text("Cómo llegar")}
        }
        OutlinedButton(onClick=onContacto){Text("Contáctanos")}
        if(historico.isNotEmpty()) {
            TextButton(onClick={verHistorico=!verHistorico}){Text("Historial anterior en este dispositivo")}
            if(verHistorico) {
                Text("Estas citas pertenecen al prototipo local. No se enviaron al servidor.")
                historico.forEach { detalle -> Text("${detalle.servicio.nombre} · ${com.example.sanaesencia.domain.model.HorariosAgenda.formatear(detalle.cita.fechaHoraInicio,"dd/MM/yyyy HH:mm")} · ${detalle.cita.estado}") }
            }
        }
    }
}
