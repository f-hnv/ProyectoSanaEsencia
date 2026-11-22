package com.example.sanaesencia.ui.administracion

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
import com.example.sanaesencia.ui.agenda.ReservasMes
import com.example.sanaesencia.viewmodel.*

@Composable
fun AdministracionServidor() {
    var tab by remember {mutableIntStateOf(0)}
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex=tab) {listOf("Cuentas","Boxes","Agendas","Citas").forEachIndexed {i,t->Tab(selected=tab==i,onClick={tab=i},text={Text(t)})}}
        when(tab) {0->AdministracionUsuariosRemotos();1->GestionBoxesServidor();2->GestionBoxesServidor(agendas=true);3->CitasAdminServidor()}
    }
}
@Composable
private fun CitasAdminServidor() {
    val app=LocalContext.current.applicationContext as SanaesenciaApp
    val vm:ListaReservasViewModel=viewModel(factory=ViewModelFactory(app.container))
    val s by vm.estado.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Citas del centro",style=MaterialTheme.typography.headlineSmall)
        ReservasMes(s,"ADMIN",vm::mes,vm::cargar,onEstado=vm::cambiarEstado)
    }
}
@Composable
private fun GestionBoxesServidor(agendas:Boolean=false) {
    val app=LocalContext.current.applicationContext as SanaesenciaApp
    val vm:AdministracionReservasViewModel=viewModel(factory=ViewModelFactory(app.container))
    val s by vm.estado.collectAsState()
    var nombre by remember {mutableStateOf("")};var descripcion by remember {mutableStateOf("")}
    var perfil by remember {mutableStateOf<Long?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(if(agendas) "Horarios de especialistas" else "Boxes del centro",style=MaterialTheme.typography.headlineSmall)
        if(s.ocupada) CircularProgressIndicator()
        s.mensaje?.let {Text(it)}
        TextButton(onClick=vm::cargar,enabled=!s.ocupada){Text("Actualizar")}
        if(agendas) {
            Text("Elige el especialista para definir su disponibilidad semanal.")
            s.perfiles.forEach {p->OutlinedButton(onClick={perfil=p.id},enabled=!s.ocupada){Text(p.nombre)}}
            perfil?.let {EditorHorarioServidor(it)}
        } else {
            s.boxes.forEach {box->Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(box.nombre);Text(box.descripcion);Text(if(box.activo) "Disponible para nuevas reservas" else "Inactivo")
                TextButton(onClick={vm.activar(box.id,!box.activo)},enabled=!s.ocupada){Text(if(box.activo) "Desactivar" else "Activar")}
            } } }
            OutlinedTextField(nombre,{nombre=it},label={Text("Nombre del box")},enabled=!s.ocupada,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(descripcion,{descripcion=it},label={Text("Descripción")},enabled=!s.ocupada,modifier=Modifier.fillMaxWidth())
            Button(onClick={vm.crear(nombre,descripcion)},enabled=!s.ocupada && nombre.isNotBlank()){Text("Crear box")}
            Text("Los boxes con reservas confirmadas futuras no pueden desactivarse.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
fun EditorHorarioServidor(perfil:Long) {
    val app=LocalContext.current.applicationContext as SanaesenciaApp
    val vm:HorariosServidorViewModel=viewModel(key="horario-$perfil",factory=ViewModelFactory(app.container))
    val s by vm.estado.collectAsState()
    LaunchedEffect(perfil){vm.cargar(perfil)}
    val dias=listOf("MONDAY" to "Lunes","TUESDAY" to "Martes","WEDNESDAY" to "Miércoles","THURSDAY" to "Jueves","FRIDAY" to "Viernes")
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Disponibilidad del especialista",style=MaterialTheme.typography.titleLarge)
        Text("Los cupos publicados se guardan para una fecha concreta, incluso sábados u otras horas. Las franjas semanales nuevas usan lunes a viernes, de 09:00 a 19:00.")
        if(s.ocupada) CircularProgressIndicator()
        s.mensaje?.let {Text(it)}
        if(!s.cargado) TextButton(onClick={vm.cargar(perfil)},enabled=!s.ocupada){Text("Reintentar horarios")}
        else {
            if(s.horarios.isEmpty()) Text("Este especialista aún no tiene horarios publicados.")
            s.horarios.forEachIndexed {i,h->
                var menu by remember(perfil,i) {mutableStateOf(false)}
                Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    h.fecha?.let {Text("Solo el ${com.example.sanaesencia.utils.FechasReservas.diaVisible(it)} · horario publicado por fecha")}
                    Box {OutlinedButton(onClick={menu=true},enabled=!s.ocupada && h.fecha==null){Text(dias.find {it.first==h.dia}?.second ?: h.dia)}
                        DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {dias.forEach {(codigo,nombre)->DropdownMenuItem(text={Text(nombre)},onClick={vm.editar(i,h.copy(dia=codigo));menu=false})}}
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(h.desde,{vm.editar(i,h.copy(desde=it))},label={Text("Desde HH:mm")},modifier=Modifier.weight(1f),singleLine=true,enabled=!s.ocupada)
                        OutlinedTextField(h.hasta,{vm.editar(i,h.copy(hasta=it))},label={Text("Hasta HH:mm")},modifier=Modifier.weight(1f),singleLine=true,enabled=!s.ocupada)
                    }
                    TextButton(onClick={vm.quitar(i)},enabled=!s.ocupada){Text("Quitar franja")}
                } }
            }
            OutlinedButton(onClick=vm::agregar,enabled=!s.ocupada && s.horarios.size<28){Text("Agregar franja")}
            Button(onClick=vm::guardar,enabled=!s.ocupada && s.horarios.isNotEmpty()){Text("Guardar horarios")}
        }
    }
}
