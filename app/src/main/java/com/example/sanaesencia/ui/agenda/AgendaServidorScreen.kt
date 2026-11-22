package com.example.sanaesencia.ui.agenda

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.SanaesenciaApp
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.utils.FechasReservas
import com.example.sanaesencia.viewmodel.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaServidorScreen(servicioLocal:Long?,perfilLocal:Long?,reserva:Long?,onVolver:()->Unit,onVerCitas:()->Unit) {
    val app=LocalContext.current.applicationContext as SanaesenciaApp
    val factory=ViewModelFactory(app.container)
    val vm:ReservaServidorViewModel=viewModel(factory=factory)
    val s by vm.estado.collectAsState()
    var pagina by remember { mutableIntStateOf(0) }
    LaunchedEffect(servicioLocal,perfilLocal,reserva) { vm.preparar(servicioLocal,perfilLocal,reserva) }
    Scaffold(topBar={ TopAppBar(title={Text(if(reserva==null) "Agendar Sesión" else "Reprogramar Sesión")},
        navigationIcon={TextButton(onClick=onVolver,enabled=!s.ocupada){Text("Volver")}}) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)) {
            val recibo=s.recibo
            if(recibo!=null) {
                Text(if(reserva==null) "Reserva confirmada" else "Reserva actualizada",style=MaterialTheme.typography.headlineSmall)
                Text("Reserva #${recibo.id}",Modifier.testTag("recibo-reserva"))
                Text(recibo.servicioNombre);Text(recibo.especialistaNombre)
                Text(FechasReservas.mostrar(recibo.inicio));Text("Fin: ${FechasReservas.mostrar(recibo.fin,"HH:mm")}")
                Text(if(recibo.modalidad==Modalidad.ONLINE) "Online · sin box" else "Presencial · ${recibo.boxNombre}",Modifier.testTag("box-confirmado"))
                Text(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL")).format(recibo.precio))
                Button(onClick=onVerCitas){Text("Ver mis citas")}
                return@Column
            }
            if(s.cargando) { CircularProgressIndicator();Text("Preparando reserva…"); return@Column }
            s.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            if(s.errorCarga) { Button(onClick=vm::reintentar){Text("Reintentar reserva")};return@Column }
            val servicio=s.servicio ?: return@Column
            Text(servicio.nombre,style=MaterialTheme.typography.titleLarge)
            Text("${servicio.duracionMinutos} minutos · ${NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL")).format(servicio.precio)}")
            Text("Elige tu especialista",style=MaterialTheme.typography.titleMedium)
            Column(Modifier.selectableGroup(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                s.profesionales.filter { reserva==null || it.id==s.especialistaId }.forEach { p ->
                    Card(Modifier.fillMaxWidth().testTag("especialista-agenda-${vm.idsLocales[p.id] ?: p.id}")
                        .selectable(selected=s.especialistaId==p.id,enabled=!s.ocupada && reserva==null,role=Role.RadioButton,onClick={vm.especialista(p.id)})) {
                        Row(Modifier.padding(12.dp)) { RadioButton(selected=s.especialistaId==p.id,onClick=null)
                            Column {Text(p.nombre);Text(p.especialidad)} }
                    }
                }
                if(s.profesionales.isEmpty()) Text("No hay especialistas activos para este servicio.")
            }
            val p=s.profesionales.find { it.id==s.especialistaId }
            val online=servicio.permiteOnline && p?.permiteOnline==true
            if(p!=null) {
                UbicacionReserva(factory,online,s.modalidad,!s.ocupada,
                    onElegirOnline={vm.modalidad(Modalidad.ONLINE)},onUbicacionCambio={ ubicacion ->
                        vm.ubicacion((ubicacion as? EstadoUbicacion.Exito)?.let { it.latitud to it.longitud })
                    })
                s.sugerencia?.let { Text(it.mensaje) };s.errorUbicacion?.let {Text(it)}
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(p.permitePresencial) FilterChip(selected=s.modalidad==Modalidad.PRESENCIAL,onClick={vm.modalidad(Modalidad.PRESENCIAL)},enabled=!s.ocupada,label={Text("Presencial")})
                    if(online) FilterChip(selected=s.modalidad==Modalidad.ONLINE,onClick={vm.modalidad(Modalidad.ONLINE)},enabled=!s.ocupada,label={Text("Online")})
                }
            }
            Text("Elige un día · hora de Chile",style=MaterialTheme.typography.titleMedium)
            Text("Se muestran las horas publicadas para el profesional y la fecha elegida. Los cupos copiados de la web son una referencia y no se sincronizan con la agenda real del centro.")
            val dias=FechasReservas.dias()
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick={pagina--},enabled=pagina>0 && !s.ocupada){Text("Anterior")}
                TextButton(onClick={pagina++},enabled=(pagina+1)*7<dias.size && !s.ocupada){Text("Más fechas")}
            }
            dias.drop(pagina*7).take(7).chunked(2).forEach { fila ->
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { fila.forEach { dia ->
                    FilterChip(selected=s.dia==dia,onClick={vm.dia(dia)},enabled=!s.ocupada,label={Text(FechasReservas.diaVisible(dia))},modifier=Modifier.testTag("fecha-$dia"))
                } }
            }
            s.dia?.let {Text("Día elegido: ${FechasReservas.diaVisible(it)}")}
            Text("Horarios disponibles",style=MaterialTheme.typography.titleMedium)
            when {
                p==null -> Text("Elige un especialista.")
                s.dia==null -> Text("Selecciona una fecha para consultar su agenda.")
                s.cargandoTurnos -> CircularProgressIndicator()
                s.errorTurnos!=null -> { Text(s.errorTurnos!!,color=MaterialTheme.colorScheme.error);TextButton(onClick=vm::cargarTurnos){Text("Reintentar horarios")} }
                s.turnos.isEmpty() -> Text("No hay horas disponibles ese día. El especialista debe tener su agenda configurada y existir un box libre para atención presencial.")
                else -> s.turnos.chunked(2).forEach { fila ->
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { fila.forEach { turno ->
                        FilterChip(selected=s.inicio==turno.inicio,onClick={vm.hora(turno.inicio)},enabled=!s.ocupada,
                            label={Text(FechasReservas.mostrar(turno.inicio,"HH:mm"))},modifier=Modifier.testTag("turno-${turno.inicio}"))
                    } }
                }
            }
            if(s.modalidad==Modalidad.PRESENCIAL) Text("El servidor asigna un box libre al confirmar. Su nombre aparecerá en la cita.")
            else Text("Sesión online · no utiliza box")
            Text("Puedes reprogramar antes del inicio. Para cancelar necesitas al menos 12 horas de anticipación.",style=MaterialTheme.typography.bodySmall)
            Button(onClick=vm::guardar,enabled=s.inicio!=null && !s.ocupada && !s.ambigua && !s.cargandoTurnos,
                modifier=Modifier.fillMaxWidth().testTag("confirmar-reserva")) {
                Text(if(s.ocupada) "Guardando…" else if(reserva==null) "Confirmar Reserva" else "Guardar Cambios")
            }
            if(s.ambigua) Button(onClick=onVerCitas){Text("Revisar mis citas")}
        }
    }
}
