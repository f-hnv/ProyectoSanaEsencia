package com.example.sanaesencia.ui.administracion
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.viewmodel.AdministracionViewModel
import com.example.sanaesencia.domain.model.HorariosAgenda
import com.example.sanaesencia.data.local.entity.UsuarioEntity
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import androidx.compose.ui.platform.testTag

@Composable
fun AdministracionScreen(vm:AdministracionViewModel=viewModel()) {
    var umbral by rememberSaveable{mutableStateOf(com.example.sanaesencia.domain.model.ConfiguracionCentro.umbralKm.value.toInt().toString())}
    var nombre by rememberSaveable{mutableStateOf("")};var email by rememberSaveable{mutableStateOf("")}
    var clave by rememberSaveable{mutableStateOf("")};var especialidad by rememberSaveable{mutableStateOf("")}
    var descripcion by rememberSaveable{mutableStateOf("")};var seleccion by remember{mutableStateOf(setOf<Long>())}
    var buscarCuenta by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(if(vm.usuario?.rol=="ADMIN") "Administración" else "Agenda del profesional",style=MaterialTheme.typography.headlineSmall)
        if(vm.usuario==null){Text("Cargando cuenta…");return@Column}
        if(vm.usuario?.rol=="PACIENTE"){Text("Esta sección está reservada al personal.");return@Column}
        vm.mensaje?.let{Text(it,color=MaterialTheme.colorScheme.primary)}
        if(vm.usuario?.rol=="ADMIN") {
            Text("Crear profesional y cuenta",style=MaterialTheme.typography.titleLarge)
            Text("Los datos se guardan en este dispositivo de demostración.",style=MaterialTheme.typography.bodySmall)
            OutlinedTextField(nombre,{nombre=it},label={Text("Nombre del profesional")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(email,{email=it},label={Text("Correo de acceso")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(clave,{clave=it},label={Text("Contraseña inicial")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(especialidad,{especialidad=it},label={Text("Especialidad clínica")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(descripcion,{descripcion=it},label={Text("Áreas y descripción")},modifier=Modifier.fillMaxWidth())
            Text("Servicios que atiende",style=MaterialTheme.typography.titleMedium)
            vm.servicios.forEach{servicio -> FilterChip(selected=servicio.id in seleccion,
                onClick={seleccion=if(servicio.id in seleccion) seleccion-servicio.id else seleccion+servicio.id},label={Text(servicio.nombre)})}
            Button(onClick={vm.crear(nombre,email,clave,especialidad,descripcion,seleccion.toList())},enabled=!vm.ocupado,
                modifier=Modifier.fillMaxWidth()){Text("Crear profesional")}
            HorizontalDivider()
            Text("Recomendación por distancia",style=MaterialTheme.typography.titleLarge)
            OutlinedTextField(umbral,{umbral=it},label={Text("Sugerir online desde (km)")},modifier=Modifier.fillMaxWidth())
            TextButton(onClick={vm.guardarUmbral(umbral)},enabled=!vm.ocupado){Text("Guardar umbral")}
            Text("No bloquea reservas: solo orienta al paciente.",style=MaterialTheme.typography.bodySmall)
            Text("Boxes de atención",style=MaterialTheme.typography.titleLarge)
            Text("Se asigna automáticamente un box libre a cada cita presencial. Online no ocupa box.",style=MaterialTheme.typography.bodySmall)
            vm.boxes.forEach{box -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("${box.nombre} · ${if(box.activo) "Habilitado" else "Deshabilitado"}")
                Switch(checked=box.activo,onCheckedChange={vm.alternarBox(box)},enabled=!vm.ocupado)
            }}
            Text("Roles de las cuentas",style=MaterialTheme.typography.titleLarge)
            Text("Paciente: puede reservar. Especialista: ve las citas de su perfil profesional por mes.")
            OutlinedTextField(buscarCuenta, { buscarCuenta = it }, label = { Text("Buscar cuenta por nombre o correo") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            vm.usuarios.filter { buscarCuenta.isBlank() || it.email.contains(buscarCuenta.trim(), true) ||
                it.nombre.contains(buscarCuenta.trim(), true) }.forEach { cuenta ->
                RolCuentaCard(cuenta, vm.especialistas.filter { profesional ->
                    vm.usuarios.none { otra -> otra.email != cuenta.email && otra.rol == "ESPECIALISTA" && otra.especialistaId == profesional.id }
                }, vm.ocupado) { rol, id -> vm.cambiarRol(cuenta.email, rol, id) }
            }
            HorizontalDivider()
        }
        Text("Citas registradas",style=MaterialTheme.typography.titleLarge)
        if(vm.citas.isEmpty()) Text("No hay citas para mostrar.")
        vm.citas.forEach{detalle -> val c=detalle.cita
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(detalle.servicio.nombre,style=MaterialTheme.typography.titleMedium)
                Text(HorariosAgenda.formatear(c.fechaHoraInicio,"dd/MM/yyyy HH:mm"))
                Text(c.emailPaciente)
                Text("${c.modalidad} · ${c.estado} · ${c.boxId?.let { "Box $it" } ?: "Sin box / online"}")
                if(c.notas.isNotBlank()) Text(c.notas)
            } }
        }
    }
}

@Composable
private fun RolCuentaCard(cuenta: UsuarioEntity, profesionales: List<EspecialistaEntity>, ocupado: Boolean,
    onGuardar: (String, Long?) -> Unit) {
    var rol by remember(cuenta.email, cuenta.rol) { mutableStateOf(cuenta.rol) }
    var profesionalId by remember(cuenta.email, cuenta.especialistaId) { mutableStateOf(cuenta.especialistaId) }
    Card(Modifier.fillMaxWidth().testTag("rol-${cuenta.email}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(cuenta.nombre, style = MaterialTheme.typography.titleMedium)
            Text(cuenta.email)
            if (cuenta.rol == "ADMIN") Text("Administrador · rol protegido")
            else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = rol == "PACIENTE", onClick = { rol = "PACIENTE" },
                        enabled = !ocupado, label = { Text("Paciente") })
                    FilterChip(selected = rol == "ESPECIALISTA", onClick = { rol = "ESPECIALISTA" },
                        enabled = !ocupado, label = { Text("Especialista") })
                }
                if (rol == "ESPECIALISTA") {
                    Text("Perfil profesional cuya agenda verá esta cuenta")
                    if (profesionales.isEmpty()) Text("No hay perfiles libres. Crea un profesional y su cuenta con el formulario superior.")
                    profesionales.forEach { profesional ->
                        FilterChip(selected = profesionalId == profesional.id,
                            onClick = { profesionalId = profesional.id }, enabled = !ocupado,
                            label = { Text("${profesional.nombre} · ${profesional.especialidad}" +
                                if (profesional.activo) "" else " · Agenda pausada") })
                    }
                }
                val id = if (rol == "ESPECIALISTA") profesionalId else null
                Button(onClick = { onGuardar(rol, id) }, enabled = !ocupado &&
                    (rol != "ESPECIALISTA" || profesionales.any { it.id == id }) &&
                    (rol != cuenta.rol || id != cuenta.especialistaId),
                    modifier = Modifier.testTag("guardar-rol-${cuenta.email}")) { Text("Guardar rol") }
            }
        }
    }
}
