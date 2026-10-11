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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sanaesencia.ui.login.LoginScreen

// Definición de las rutas solicitadas
sealed class Rutas(val ruta: String) {
    object Login : Rutas("login")
    object Registro : Rutas("registro")
    object Home : Rutas("home")
    object CatalogoServicios : Rutas("catalogo_servicios")
    object Agenda : Rutas("agenda")
    object Perfil : Rutas("perfil")
}

@Composable
fun SanaesenciaNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Rutas.Login.ruta,
        // Implementación de animación funcional para transiciones de entrada y salida
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeOut(tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut(tween(300))
        }
    ) {
        composable(Rutas.Login.ruta) {LoginScreen(
            onNavigateToHome = { navController.navigate(Rutas.Home.ruta) },
            onNavigateToRegistro = { navController.navigate(Rutas.Registro.ruta) }
        )
        }
        composable(Rutas.Registro.ruta) { PlaceholderScreen("Registro") }
        composable(Rutas.Home.ruta) { PlaceholderScreen("Dashboard / Próximas Citas") }
        composable(Rutas.CatalogoServicios.ruta) { PlaceholderScreen("Catálogo de Servicios") }
        composable(Rutas.Agenda.ruta) { PlaceholderScreen("Agenda") }
        composable(Rutas.Perfil.ruta) { PlaceholderScreen("Perfil") }
    }
}

// Vista temporal para probar que la navegación no crashee
@Composable
fun PlaceholderScreen(titulo: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}