package com.example.sanaesencia.ui.components
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.sanaesencia.data.repository.CuentaRepository
import com.example.sanaesencia.data.local.entity.UsuarioEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanaShell(nav:NavHostController,contenido:@Composable ()->Unit) {
    val entrada by nav.currentBackStackEntryAsState()
    val ruta=entrada?.destination?.route.orEmpty()
    val context=LocalContext.current
    val repo=remember{CuentaRepository(context)}
    var usuario by remember{mutableStateOf<UsuarioEntity?>(null)}
    var transicion by remember{mutableStateOf(false)}
    val drawer=rememberDrawerState(DrawerValue.Closed)
    val scope=rememberCoroutineScope()
    val autenticada=ruta.isNotBlank() && ruta!="login" && ruta!="registro"
    LaunchedEffect(ruta){
        usuario=repo.actual()
        if(autenticada){transicion=true;delay(220);transicion=false}
    }
    fun ir(destino:String){
        scope.launch{drawer.close()}
        if(ruta!=destino) nav.navigate(destino){
            popUpTo("home");launchSingleTop=true
        }
    }
    ModalNavigationDrawer(drawerState=drawer,gesturesEnabled=autenticada,drawerContent={
        ModalDrawerSheet {
            LogoSanaesencia(Modifier.padding(24.dp).fillMaxWidth(.85f))
            Text(usuario?.nombre.orEmpty(),Modifier.padding(horizontal=24.dp),style=MaterialTheme.typography.titleMedium)
            Text(usuario?.rol?.lowercase().orEmpty(),Modifier.padding(24.dp),style=MaterialTheme.typography.labelMedium)
            listOf("Inicio" to "home","Servicios y reserva" to "catalogo_servicios","Mensajes" to "mensajes",
                "Mi perfil" to "perfil","Cómo llegar" to "sede","Contáctanos" to "contacto").forEach{(titulo,destino)->
                NavigationDrawerItem(label={Text(titulo)},selected=ruta==destino,onClick={ir(destino)},modifier=Modifier.padding(horizontal=12.dp))
            }
            if(usuario!=null && usuario?.rol!="PACIENTE") NavigationDrawerItem(
                label={Text(if(usuario?.rol=="ADMIN") "Administración" else "Mi agenda profesional")},selected=ruta=="administracion",
                onClick={ir("administracion")},modifier=Modifier.padding(horizontal=12.dp))
            HorizontalDivider(Modifier.padding(16.dp))
            TextButton(onClick={repo.salir();scope.launch{drawer.close()};nav.navigate("login"){popUpTo(nav.graph.id){inclusive=true};launchSingleTop=true}}){Text("Cerrar sesión")}
        }
    }) {
        Scaffold(topBar={if(autenticada) TopAppBar(title={LogoSanaesencia(Modifier.width(200.dp))},
            navigationIcon={IconButton(onClick={scope.launch{drawer.open()}}){Icon(Icons.Default.Menu,contentDescription="Abrir menú")}})},
            bottomBar={if(autenticada) NavigationBar {
                listOf(Triple("home","Inicio",Icons.Default.Home),Triple("catalogo_servicios","Servicios",Icons.Default.DateRange),
                    Triple("mensajes","Mensajes",Icons.Default.Email),Triple("perfil","Perfil",Icons.Default.Person)).forEach{(destino,titulo,icono)->
                    NavigationBarItem(modifier=Modifier.testTag("nav-$destino"),selected=ruta==destino || (destino=="catalogo_servicios" && ruta.startsWith("agenda")),
                        onClick={ir(destino)},icon={Icon(icono,contentDescription=null)},label={Text(titulo)})
                }
            }}){padding -> Box(Modifier.fillMaxSize().padding(padding)) {
                contenido()
                AnimatedVisibility(transicion,modifier=Modifier.align(Alignment.Center)) {
                    Surface(shape=MaterialTheme.shapes.extraLarge,shadowElevation=6.dp,
                        color=MaterialTheme.colorScheme.surface.copy(alpha=.97f)) {
                        Box(Modifier.padding(24.dp)){CargaSanaesencia("Abriendo sección…")}
                    }
                }
            }}
    }
}
