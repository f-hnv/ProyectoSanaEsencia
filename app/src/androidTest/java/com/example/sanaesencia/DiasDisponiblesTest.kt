package com.example.sanaesencia

import androidx.compose.ui.test.*
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.sanaesencia.ui.agenda.SelectorDiasDisponibles
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class DiasDisponiblesTest {
    @get:Rule val compose=createComposeRule()
    @Test fun soloOfreceLasFechasConCuposYPermiteElegirlas() {
        var elegida:String?=null
        compose.setContent { SanaesenciaTheme { Column {
            SelectorDiasDisponibles(listOf("2027-01-15","2027-01-19"),null,false,null,true,{elegida=it},{})
        } } }
        compose.onNodeWithTag("fecha-2027-01-15").assertExists()
        compose.onNodeWithTag("fecha-2027-01-16").assertDoesNotExist()
        compose.onNodeWithTag("fecha-2027-01-19").performClick()
        compose.runOnIdle { assertEquals("2027-01-19",elegida) }
    }
}
