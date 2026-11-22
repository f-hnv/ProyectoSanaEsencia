package com.example.sanaesencia.ui.agenda

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.sanaesencia.data.remote.ReservaRemota
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.utils.FechasReservas
import com.example.sanaesencia.viewmodel.*

@Composable
fun ReservasMes(s:ListaReservasState,rol:String,onMes:(Int)->Unit,onRecargar:()->Unit,
    onEditar:(Long)->Unit={},onEstado:(Long,Boolean)->Unit) {
    var confirmar by remember { mutableStateOf<ReservaRemota?>(null) }
    var atendida by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        TextButton(onClick={onMes(-1)},enabled=!s.ocupada,modifier=Modifier.testTag("mes-anterior")){Text("Mes anterior")}
        TextButton(onClick={onMes(1)},enabled=!s.ocupada,modifier=Modifier.testTag("mes-siguiente")){Text("Mes siguiente")}
    }
    Text(s.periodo.titulo,Modifier.testTag("mes-agenda"),style=MaterialTheme.typography.titleLarge)
    TextButton(onClick=onRecargar,enabled=!s.ocupada){Text("Actualizar citas")}
    s.mensaje?.let {Text(it)}
    when {
        s.cargando -> { CircularProgressIndicator();Text("Cargando citas del mes…") }
        s.error!=null -> { Text(s.error,color=MaterialTheme.colorScheme.error);TextButton(onClick=onRecargar){Text("Reintentar agenda")} }
        else -> {
            Text("${s.citas.size} citas en el mes · ${s.citas.count { it.estado=="CONFIRMADA" }} programadas")
            if(s.citas.isEmpty()) Text(if(rol=="ESPECIALISTA") "No tienes citas agendadas para este mes." else "No tienes citas registradas para este mes.")
            s.citas.forEach { cita ->
                Card(Modifier.fillMaxWidth().testTag(if(rol=="ESPECIALISTA") "cita-terapeuta-${cita.id}" else "reserva-${cita.id}")) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(cita.servicioNombre,style=MaterialTheme.typography.titleMedium)
                        Text("Reserva #${cita.id}");Text(FechasReservas.mostrar(cita.inicio))
                        Text("Fin: ${FechasReservas.mostrar(cita.fin,"HH:mm")}")
                        Text("Especialista: ${cita.especialistaNombre}")
                        if(rol!="PACIENTE") Text("Paciente: ${cita.pacienteNombre}")
                        Text(if(cita.modalidad==Modalidad.ONLINE) "Online · sin box" else "Presencial · ${cita.boxNombre}")
                        Text("Estado: ${cita.estado.lowercase()}")
                        if(FechasReservas.futura(cita)) {
                            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                if(rol=="PACIENTE") OutlinedButton(onClick={onEditar(cita.id)},enabled=!s.ocupada){Text("Modificar")}
                                OutlinedButton(onClick={confirmar=cita;atendida=false},enabled=!s.ocupada && FechasReservas.cancelable(cita)){Text("Cancelar")}
                            }
                            if(!FechasReservas.cancelable(cita)) Text("La cancelación requiere al menos 12 horas de anticipación.",style=MaterialTheme.typography.bodySmall)
                        }
                        if(rol!="PACIENTE" && cita.estado=="CONFIRMADA" && FechasReservas.instante(cita.fin)<=System.currentTimeMillis()) {
                            OutlinedButton(onClick={confirmar=cita;atendida=true},enabled=!s.ocupada){Text("Marcar atendida")}
                        }
                    }
                }
            }
        }
    }
    confirmar?.let { cita -> AlertDialog(onDismissRequest={confirmar=null},title={Text(if(atendida) "Marcar sesión atendida" else "Cancelar reserva")},
        text={Text("${cita.servicioNombre} · ${FechasReservas.mostrar(cita.inicio)}")},
        confirmButton={TextButton(onClick={onEstado(cita.id,atendida);confirmar=null}){Text("Confirmar")}},
        dismissButton={TextButton(onClick={confirmar=null}){Text("Volver")}}) }
}
