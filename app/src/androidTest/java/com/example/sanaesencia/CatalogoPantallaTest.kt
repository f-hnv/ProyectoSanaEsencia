package com.example.sanaesencia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.repository.CatalogoEstado
import com.example.sanaesencia.ui.catalogo.CatalogoContent
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CatalogoPantallaTest {
    @get:Rule val compose=createComposeRule()
    private fun estado()=CatalogoEstado(servicios=listOf(ServicioEntity(701,"Servicio del servidor","Descripción","Centro",75,34568,precioServidor="34567.50")),
        especialistas=listOf(EspecialistaEntity(902,"Terapeuta asignada","Especialidad"),EspecialistaEntity(903,"Terapeuta de otro servicio","Otra")),
        serviciosPorEspecialista=mapOf(902L to listOf(701L),903L to listOf(999L)),cargando=false,vigente=true)
    @Test fun listaVaciaExplicaQueNoHayServicios() {
        compose.setContent { SanaesenciaTheme { CatalogoContent(CatalogoEstado(cargando=false,vigente=true),{},{_,_->}) } }
        compose.onNodeWithTag("catalogo-vacio").assertExists()
        compose.onNodeWithText("No hay servicios disponibles por ahora.").assertExists()
    }
    @Test fun muestraPrecioDuracionYSeleccionSoloParaEspecialistasAsignados() {
        var elegidos:Pair<Long,Long>?=null
        compose.setContent { SanaesenciaTheme { CatalogoContent(estado(),{},{s,p->elegidos=s to p}) } }
        compose.onNodeWithText("75 minutos ·",substring=true).assertExists()
        compose.onNodeWithText("34.567,50",substring=true).assertExists()
        compose.onNodeWithTag("ver-especialistas-701").performScrollTo().performClick()
        compose.onNodeWithText("Terapeuta de otro servicio").assertDoesNotExist()
        compose.onNodeWithTag("elegir-701-902").performScrollTo().performClick()
        assertEquals(701L to 902L,elegidos)
    }
    @Test fun copiaGuardadaSePuedeLeerPeroNoElegirHastaActualizar() {
        var reintentos=0
        compose.setContent { SanaesenciaTheme { CatalogoContent(estado().copy(vigente=false,error="No hay conexión",actualizadoEn=100),{},{_,_->},{reintentos++}) } }
        compose.onNodeWithTag("catalogo-error").assertExists()
        compose.onNodeWithText("Reintentar").performScrollTo().performClick(); assertEquals(1,reintentos)
        compose.onNodeWithTag("ver-especialistas-701").performScrollTo().performClick()
        compose.onNodeWithTag("elegir-701-902").performScrollTo().assertIsNotEnabled()
    }
    @Test fun duranteCargaSeInformaElEstadoYNoSeDuplicanActualizaciones() {
        compose.setContent { SanaesenciaTheme { CatalogoContent(CatalogoEstado(),{},{_,_->}) } }
        compose.onNodeWithText("Cargando catálogo…").assertExists()
        compose.onNodeWithTag("catalogo-actualizar").assertIsNotEnabled()
        compose.onNodeWithTag("catalogo-vacio").assertDoesNotExist()
    }
}
