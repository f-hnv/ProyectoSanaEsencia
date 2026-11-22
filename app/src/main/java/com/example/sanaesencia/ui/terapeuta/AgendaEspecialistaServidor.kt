package com.example.sanaesencia.ui.terapeuta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.SanaesenciaApp
import com.example.sanaesencia.data.remote.SesionBackend
import com.example.sanaesencia.ui.agenda.ReservasMes
import com.example.sanaesencia.ui.administracion.EditorHorarioServidor
import com.example.sanaesencia.viewmodel.*

@Composable
fun AgendaEspecialistaServidor() {
    val app=LocalContext.current.applicationContext as SanaesenciaApp
    val vm:ListaReservasViewModel=viewModel(factory=ViewModelFactory(app.container))
    val estado by vm.estado.collectAsState()
    var horarios by remember {mutableStateOf(false)}
    LaunchedEffect(Unit){vm.cargar()}
    Column(Modifier.fillMaxSize().testTag("agenda-terapeuta").verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Mi agenda mensual",style=MaterialTheme.typography.headlineSmall)
        Text("Citas de tu perfil profesional · hora de Chile")
        OutlinedButton(onClick={horarios=!horarios}){Text(if(horarios) "Ver citas" else "Configurar mis horarios")}
        if(horarios) SesionBackend.actual()?.usuario?.especialistaId?.let {EditorHorarioServidor(it)}
        else ReservasMes(estado,"ESPECIALISTA",vm::mes,vm::cargar,onEstado=vm::cambiarEstado)
    }
}
