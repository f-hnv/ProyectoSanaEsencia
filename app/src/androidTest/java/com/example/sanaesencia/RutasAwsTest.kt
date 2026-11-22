package com.example.sanaesencia

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.repository.RutasRepository
import com.example.sanaesencia.ui.sede.RecorridoContent
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.viewmodel.*
import kotlinx.coroutines.*
import okhttp3.Credentials
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.UUID

class RutasAwsTest {
    @get:Rule val compose=createComposeRule()
    private val correo="ruta.aws.${UUID.randomUUID()}@example.com"
    private val clave="RutaAws${UUID.randomUUID()}!"
    private var id:Long?=null
    private var vm:RutasViewModel?=null
    interface Limpieza {
        @DELETE("api/usuarios/{id}") suspend fun desactivar(@Path("id") id:Long,@Header("Authorization") auth:String)
    }
    @Before fun preparar()=runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("awsE2e")=="true")
        assertTrue(BuildConfig.RESERVAS_API_URL.startsWith("https://"))
        val cuerpo=(50_000_000L+UUID.randomUUID().mostSignificantBits.and(Long.MAX_VALUE)%9_000_000).toString()
        val suma=cuerpo.reversed().mapIndexed {i,c->c.digitToInt()*(2+i%6)}.sum()
        val dv=when(val d=11-suma%11){11->"0";10->"K";else->d.toString()}
        val usuario=ConexionBackend.usuarios.registrar(RegistroRequest("Prueba","Recorrido AWS","$cuerpo-$dv",correo,clave))
        id=usuario.id
        SesionBackend.abrir(ConexionBackend.usuarios.login(LoginRequest(correo,clave)),clave)
    }
    @After fun limpiar():Unit=runBlocking {
        vm?.viewModelScope?.cancel()
        try{id?.let {Retrofit.Builder().baseUrl(BuildConfig.USUARIOS_API_URL).addConverterFactory(GsonConverterFactory.create()).build()
            .create(Limpieza::class.java).desactivar(it,Credentials.basic(correo,clave,Charsets.UTF_8))}}
        finally{SesionBackend.cerrar()}
        Unit
    }
    @Test fun consultaRealDesdePantallaSinClaveDelProveedorEnAndroid(){
        val modelo=RutasViewModel(RutasRepository(ConexionRutas.api));vm=modelo
        compose.setContent {val estado by modelo.estado.collectAsState();SanaesenciaTheme {RecorridoContent(estado){modelo.consultar(-31.9,-71.5)}}}
        compose.onNodeWithText("Al consultar, compartes tu ubicación",substring=true).assertExists()
        compose.onNodeWithText("Consultar recorrido con TrueWay").performClick()
        compose.waitUntil(30_000){modelo.estado.value is RutaUiState.Exito || modelo.estado.value is RutaUiState.Error}
        val estado=modelo.estado.value
        assertTrue("La consulta HTTPS de AWS no terminó correctamente: $estado",estado is RutaUiState.Exito)
        val recorrido=(estado as RutaUiState.Exito).recorrido
        assertTrue(recorrido.metros>0);assertTrue(recorrido.segundos>0)
        compose.onNodeWithText("${recorrido.distanciaTexto} · ${recorrido.duracionTexto}").assertExists()
        assertFalse(BuildConfig::class.java.declaredFields.any {it.name=="TRUEWAY_API_KEY"})
    }
}
