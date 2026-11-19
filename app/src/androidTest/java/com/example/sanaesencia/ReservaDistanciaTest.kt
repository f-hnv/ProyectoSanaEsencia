package com.example.sanaesencia

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.ui.agenda.UbicacionReservaContent
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.viewmodel.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReservaDistanciaTest {
    @get:Rule val compose = createComposeRule()

    @Test fun distanciaLejanaOfreceOnlineYLaEleccionEsExplicita() {
        val modalidad = mutableStateOf(Modalidad.PRESENCIAL)
        compose.setContent { SanaesenciaTheme {
            UbicacionReservaContent(
                SedeUiState(EstadoUbicacion.Exito(-33.4, -70.6, 10f), 200_000.0, "200 km"),
                20f, true, modalidad.value, true, {}, { modalidad.value = Modalidad.ONLINE })
        } }
        compose.onNodeWithText("Estás a más de 20 km", substring = true).assertExists()
        assertEquals(Modalidad.PRESENCIAL, modalidad.value)
        compose.onNodeWithText("Elegir sesión online").performClick()
        compose.onNodeWithText("Seleccionaste atención online · sin box").assertExists()
        assertEquals(Modalidad.ONLINE, modalidad.value)
    }

    @Test fun servicioSoloPresencialNoOfreceOnlineAunqueEsteLejos() {
        compose.setContent { SanaesenciaTheme {
            UbicacionReservaContent(SedeUiState(EstadoUbicacion.Exito(0.0, 0.0, 20f), 1_000_000.0, "1000 km"),
                20f, false, Modalidad.PRESENCIAL, true, {}, {})
        } }
        compose.onNodeWithText("este servicio solo admite atención presencial", substring = true).assertExists()
        compose.onNodeWithText("Elegir sesión online").assertDoesNotExist()
    }

    @Test fun sinPermisoSePuedeReintentarSinBloquearLaReserva() {
        var consultas = 0
        compose.setContent { SanaesenciaTheme {
            UbicacionReservaContent(SedeUiState(EstadoUbicacion.PermisoDenegado), 20f, true,
                Modalidad.PRESENCIAL, true, { consultas++ }, {})
        } }
        compose.onNodeWithText("Puedes reservar", substring = true).assertExists()
        compose.onNodeWithText("Usar mi ubicación").performClick()
        assertEquals(1, consultas)
    }
}
