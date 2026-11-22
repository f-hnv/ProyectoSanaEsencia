package com.example.sanaesencia.ui.administracion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.viewmodel.UsuariosAdminViewModel

@Composable
fun AdministracionUsuariosRemotos(vm: UsuariosAdminViewModel = viewModel()) {
    var id by rememberSaveable { mutableStateOf("") }
    var rol by remember(vm.cuenta) { mutableStateOf(vm.cuenta?.rol ?: "PACIENTE") }
    var perfilId by remember(vm.cuenta) { mutableStateOf(vm.cuenta?.especialistaId?.toString().orEmpty()) }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Administración", style=MaterialTheme.typography.headlineSmall)
        Text("Roles de las cuentas", style=MaterialTheme.typography.titleLarge)
        Text("El número de cuenta aparece en Mi perfil. La cuenta debe registrarse antes de asignarle un rol.")
        OutlinedTextField(id, { id=it }, label={Text("Número de cuenta")}, singleLine=true,
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), enabled=!vm.ocupado, modifier=Modifier.fillMaxWidth())
        Button(onClick={vm.buscar(id)}, enabled=!vm.ocupado) { Text("Buscar cuenta") }
        if (vm.ocupado) CircularProgressIndicator()
        vm.mensaje?.let { Text(it) }
        vm.cuenta?.let { cuenta ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${cuenta.nombre} ${cuenta.apellido}", style=MaterialTheme.typography.titleMedium)
                Text(cuenta.correo)
                Text("Rol actual: ${cuenta.rol.lowercase()}")
                if (cuenta.rol == "ADMIN") Text("Administrador · rol protegido")
                else if (!cuenta.activo) Text("Esta cuenta está desactivada")
                else {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(rol=="PACIENTE", {rol="PACIENTE"}, label={Text("Paciente")}, enabled=!vm.ocupado)
                        FilterChip(rol=="ESPECIALISTA", {rol="ESPECIALISTA"}, label={Text("Especialista")}, enabled=!vm.ocupado)
                    }
                    if (rol=="ESPECIALISTA") {
                        OutlinedTextField(perfilId, {perfilId=it}, label={Text("Número del perfil profesional")},
                            supportingText={Text("Debe corresponder a un especialista activo del catálogo del centro.")},
                            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), enabled=!vm.ocupado)
                    }
                    Button(onClick={vm.guardar(rol,perfilId)}, enabled=!vm.ocupado &&
                        (rol != cuenta.rol || (if(rol=="ESPECIALISTA") perfilId.toLongOrNull() else null) != cuenta.especialistaId)) { Text("Guardar rol") }
                }
            } }
        }
    }
}
