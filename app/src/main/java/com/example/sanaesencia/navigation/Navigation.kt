package com.example.sanaesencia.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.tooling.preview.Preview
import com.example.sanaesencia.ui.login.LoginScreen
import com.example.sanaesencia.ui.registro.RegistroScreen
import com.example.sanaesencia.ui.perfil.PerfilScreen
import com.example.sanaesencia.ui.home.HomeScreen
import com.example.sanaesencia.ui.catalogo.CatalogoScreen
import com.example.sanaesencia.ui.agenda.AgendaScreen
import com.example.sanaesencia.ui.sede.SedeScreen
import com.example.sanaesencia.ui.contacto.ContactoScreen
import com.example.sanaesencia.ui.theme.SanaesenciaTheme

sealed class Rutas(val ruta: String) {
    object Login : Rutas("login")
    object Registro : Rutas("registro")
    object Home : Rutas("home")
    object CatalogoServicios : Rutas("catalogo_servicios")

    // Ahora acepta servicioId y citaId para la edición
    object Agenda : Rutas("agenda?servicioId={servicioId}&citaId={citaId}") {
        fun crearRuta(servicioId: Long?, citaId: Long?) =
            "agenda?servicioId=${servicioId?.toString() ?: ""}&citaId=${citaId?.toString() ?: ""}"
    }

    object Perfil : Rutas("perfil")
    object Sede : Rutas("sede")
    object Contacto : Rutas("contacto")
}

@Composable
fun SanaesenciaNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Rutas.Login.ruta,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn(tween(300)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeOut(tween(300)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeIn(tween(300)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut(tween(300)) }
    ) {
        composable(Rutas.Login.ruta) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Rutas.Home.ruta) {
                        popUpTo(Rutas.Login.ruta) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegistro = { navController.navigate(Rutas.Registro.ruta) },
                onNavigateToContacto = { navController.navigate(Rutas.Contacto.ruta) }
            )
        }
        composable(Rutas.Registro.ruta) {
            RegistroScreen(
                onNavigateToLogin = { navController.navigate(Rutas.Login.ruta) { popUpTo(Rutas.Login.ruta) { inclusive = true } } }
            )
        }
        composable(Rutas.Home.ruta) {
            HomeScreen(
                onNavigateToCatalogo = { navController.navigate(Rutas.CatalogoServicios.ruta) },
                onNavigateToAgenda = { servId, citId ->
                    // Una reserva nueva necesita elegir primero el servicio y su duración.
                    if (servId == null && citId == null) navController.navigate(Rutas.CatalogoServicios.ruta)
                    else navController.navigate(Rutas.Agenda.crearRuta(servId, citId))
                },
                onNavigateToPerfil = { navController.navigate(Rutas.Perfil.ruta) },
                onNavigateToSede = { navController.navigate(Rutas.Sede.ruta) },
                onNavigateToContacto = { navController.navigate(Rutas.Contacto.ruta) }
            )
        }
        composable(Rutas.CatalogoServicios.ruta) {
            CatalogoScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAgenda = { servicioId -> navController.navigate(Rutas.Agenda.crearRuta(servicioId, null)) }
            )
        }

        composable(
            route = Rutas.Agenda.ruta,
            arguments = listOf(
                navArgument("servicioId") { type = NavType.StringType; nullable = true },
                navArgument("citaId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val servicioIdStr = backStackEntry.arguments?.getString("servicioId")
            val citaIdStr = backStackEntry.arguments?.getString("citaId")

            val servId = if (servicioIdStr.isNullOrBlank()) null else servicioIdStr.toLongOrNull()
            val citId = if (citaIdStr.isNullOrBlank()) null else citaIdStr.toLongOrNull()

            AgendaScreen(
                servicioIdPreseleccionado = servId,
                citaIdAEditar = citId,
                onNavigateToSede = { navController.navigate(Rutas.Sede.ruta) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Rutas.Perfil.ruta) {
            PerfilScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = { navController.navigate(Rutas.Login.ruta) { popUpTo(Rutas.Home.ruta) { inclusive = true } } }
            )
        }
        composable(Rutas.Sede.ruta) {
            SedeScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Rutas.Contacto.ruta) {
            ContactoScreen(onNavigateBack = { navController.popBackStack() },
                onNavigateToSede = { navController.navigate(Rutas.Sede.ruta) })
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NavegacionCompletaPreview() {
    SanaesenciaTheme {
        val navController = rememberNavController()
        SanaesenciaNavigation(navController = navController)
    }
}
