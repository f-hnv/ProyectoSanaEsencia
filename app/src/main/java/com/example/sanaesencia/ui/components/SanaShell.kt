package com.example.sanaesencia.ui.components
import androidx.compose.animation.AnimatedVisibility
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
import com.example.sanaesencia.domain.model.VistasCuenta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanaShell(nav:NavHostController,contenido:@Composable (UsuarioEntity?)->Unit) {
    val entrada by nav.currentBackStackEntryAsState()
    val ruta=entrada?.destination?.route.orEmpty()
    val context=LocalContext.current
    val repo=remember{CuentaRepository(context)}
    var usuario by remember{mutableStateOf<UsuarioEntity?>(null)}
    var cargandoCuenta by remember { mutableStateOf(true) }
    var transicion by remember{mutableStateOf(false)}
    val drawer=rememberDrawerState(DrawerValue.Closed)
    val scope=rememberCoroutineScope()
    val publica = ruta in listOf("", "login", "registro", "contacto", "sede")
    val autenticada=usuario != null && ruta!="login" && ruta!="registro"
    LaunchedEffect(ruta){
        cargandoCuenta = true
        try { usuario=repo.actual() }
        finally { cargandoCuenta = false }
        if(autenticada){transicion=true;delay(220);transicion=false}
    }
    val destinos = when (usuario?.rol) {
        "PACIENTE" -> listOf("Inicio" to "home", "Servicios y reserva" to "catalogo_servicios", "Mensajes" to "mensajes",
            "Mi perfil" to "perfil", "Cómo llegar" to "sede", "Contáctanos" to "contacto")
        "ESPECIALISTA" -> listOf("Mi agenda mensual" to "home", "Mensajes" to "mensajes", "Mi perfil" to "perfil")
        "ADMIN" -> listOf("Administración" to "home", "Mensajes" to "mensajes", "Mi perfil" to "perfil")
        else -> emptyList()
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
            destinos.forEach{(titulo,destino)->
                NavigationDrawerItem(label={Text(titulo)},selected=ruta==destino,onClick={ir(destino)},modifier=Modifier.padding(horizontal=12.dp))
            }
            HorizontalDivider(Modifier.padding(16.dp))
            TextButton(onClick={repo.salir();scope.launch{drawer.close()};nav.navigate("login"){popUpTo(nav.graph.id){inclusive=true};launchSingleTop=true}}){Text("Cerrar sesión")}
        }
    }) {
        Scaffold(topBar={if(autenticada) TopAppBar(title={LogoSanaesencia(Modifier.width(200.dp))},
            navigationIcon={IconButton(onClick={scope.launch{drawer.open()}}){Icon(Icons.Default.Menu,contentDescription="Abrir menú")}})},
            bottomBar={if(autenticada) NavigationBar {
                val barra = buildList {
                    add(Triple("home", when(usuario?.rol) { "ESPECIALISTA" -> "Mi agenda"; "ADMIN" -> "Administración"; else -> "Inicio" }, Icons.Default.Home))
                    if (VistasCuenta.puedeReservar(usuario?.rol)) add(Triple("catalogo_servicios","Servicios",Icons.Default.DateRange))
                    add(Triple("mensajes","Mensajes",Icons.Default.Email))
                    add(Triple("perfil","Perfil",Icons.Default.Person))
                }
                barra.forEach{(destino,titulo,icono)->
                    NavigationBarItem(modifier=Modifier.testTag("nav-$destino"),selected=ruta==destino || (destino=="catalogo_servicios" && ruta.startsWith("agenda")),
                        onClick={ir(destino)},icon={Icon(icono,contentDescription=null)},label={Text(titulo)})
                }
            }}){padding -> Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    !publica && cargandoCuenta -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    !publica && !VistasCuenta.puedeVerRuta(usuario?.rol, ruta) -> Column(Modifier.padding(24.dp)) {
                        Text(if (usuario == null) "Inicia sesión para continuar" else "Esta sección no corresponde al rol de tu cuenta")
                        TextButton(onClick = { ir(if (usuario == null) "login" else "home") }) { Text("Volver") }
                    }
                    else -> Column(Modifier.fillMaxSize()) {
                        if (autenticada && com.example.sanaesencia.BuildConfig.DEBUG && usuario?.rol != "ADMIN") {
                            Text("Las citas se guardan en el servidor. Los mensajes siguen disponibles solo en este dispositivo.",
                                Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=8.dp),
                                style=MaterialTheme.typography.bodySmall)
                        }
                        Box(Modifier.weight(1f)) { contenido(usuario) }
                    }
                }
                AnimatedVisibility(transicion,modifier=Modifier.align(Alignment.Center)) {
                    Surface(shape=MaterialTheme.shapes.extraLarge,shadowElevation=6.dp,
                        color=MaterialTheme.colorScheme.surface.copy(alpha=.97f)) {
                        Box(Modifier.padding(24.dp)){CargaSanaesencia("Abriendo sección…")}
                    }
                }
            }}
    }
}
