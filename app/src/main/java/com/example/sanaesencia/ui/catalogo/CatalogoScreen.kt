package com.example.sanaesencia.ui.catalogo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.SanaesenciaApp
import com.example.sanaesencia.data.local.entity.ServicioEntity
import com.example.sanaesencia.data.repository.CatalogoEstado
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.utils.precioFormateado
import com.example.sanaesencia.viewmodel.CatalogoViewModel
import com.example.sanaesencia.viewmodel.ViewModelFactory

@Composable
fun CatalogoScreen(onNavigateBack:()->Unit,onNavigateToAgenda:(Long,Long)->Unit) {
    val app=LocalContext.current.applicationContext as SanaesenciaApp
    val vm:CatalogoViewModel=viewModel(factory=ViewModelFactory(app.container))
    val estado by vm.uiState.collectAsState()
    CatalogoContent(estado,onNavigateBack,onNavigateToAgenda,vm::actualizar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoContent(estado:CatalogoEstado,onNavigateBack:()->Unit,
    onNavigateToAgenda:(Long,Long)->Unit,onActualizar:()->Unit={}) {
    Scaffold(topBar={TopAppBar(title={Text("Nuestros Servicios")},
        navigationIcon={IconButton(onClick=onNavigateBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Volver")}},
        actions={TextButton(onClick=onActualizar,enabled=!estado.cargando,modifier=Modifier.testTag("catalogo-actualizar")){Text("Actualizar")}},
        colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp).testTag("catalogo"),
            contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(estado.cargando) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Cargando catálogo…") }
            estado.error?.let { mensaje -> item {
                Column(Modifier.testTag("catalogo-error")) {
                    Text(mensaje,color=MaterialTheme.colorScheme.error)
                    if(estado.actualizadoEn!=null) Text("Mostrando la última copia guardada. Actualiza antes de elegir una sesión.")
                    TextButton(onClick=onActualizar,enabled=!estado.cargando){Text("Reintentar")}
                }
            } }
            if(!estado.cargando && estado.error==null && estado.servicios.isEmpty()) item {
                Text("No hay servicios disponibles por ahora.",Modifier.testTag("catalogo-vacio"))
            }
            items(estado.servicios,key={it.id}) { servicio ->
                var expandido by rememberSaveable(servicio.id) { mutableStateOf(false) }
                val profesionales=estado.especialistas.filter { servicio.id in estado.serviciosPorEspecialista[it.id].orEmpty() }
                Card(Modifier.fillMaxWidth().testTag("servicio-catalogo-${servicio.id}"),
                    colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
                    border=BorderStroke(1.5.dp,MaterialTheme.colorScheme.secondary.copy(alpha=.6f))) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(servicio.nombre,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(servicio.descripcion,style=MaterialTheme.typography.bodyMedium)
                        Text("${servicio.duracionMin} minutos · ${servicio.precioFormateado()}",
                            style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                        Text(if(servicio.permiteOnline) "Consulta presencial u online según especialista" else "Consulta presencial",
                            style=MaterialTheme.typography.bodySmall)
                        TextButton(onClick={expandido=!expandido},modifier=Modifier.testTag("ver-especialistas-${servicio.id}")) {
                            Text(if(expandido) "Ocultar especialistas" else "Ver especialistas")
                        }
                        if(expandido) {
                            if(profesionales.isEmpty()) Text("No hay especialistas disponibles para este servicio.")
                            profesionales.forEach { profesional ->
                                HorizontalDivider()
                                Text(profesional.nombre,style=MaterialTheme.typography.titleSmall)
                                Text(profesional.especialidad)
                                if(profesional.descripcion.isNotBlank()) Text(profesional.descripcion,style=MaterialTheme.typography.bodySmall)
                                val modalidades=buildList {
                                    if(profesional.permitePresencial) add("Presencial")
                                    if(servicio.permiteOnline && profesional.permiteOnline) add("Online")
                                }
                                Text(modalidades.joinToString(" · "),style=MaterialTheme.typography.labelMedium)
                                Button(onClick={onNavigateToAgenda(servicio.id,profesional.id)},
                                    enabled=estado.vigente && !estado.cargando && modalidades.isNotEmpty(),
                                    modifier=Modifier.testTag("elegir-${servicio.id}-${profesional.id}")) { Text("Elegir a ${profesional.nombre}") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground=true)
@Composable
fun CatalogoScreenPreview() {
    SanaesenciaTheme { CatalogoContent(CatalogoEstado(servicios=listOf(ServicioEntity(id=1,nombre="Terapia Psicológica",
        descripcion="Sesión individual.",categoria="Servicios del centro",duracionMin=60,precioClp=30000)),cargando=false,vigente=true),{},{_,_->}) }
}
