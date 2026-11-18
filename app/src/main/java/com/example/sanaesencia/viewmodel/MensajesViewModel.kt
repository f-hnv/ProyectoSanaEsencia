package com.example.sanaesencia.viewmodel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.repository.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MensajesViewModel(app: Application): AndroidViewModel(app) {
    private val cuenta = CuentaRepository(app)
    private val repo = MensajeRepository(app)
    var usuario by mutableStateOf<UsuarioEntity?>(null); private set
    var pacientes by mutableStateOf<List<UsuarioEntity>>(emptyList()); private set
    var seleccionado by mutableStateOf<String?>(null); private set
    var mensajes by mutableStateOf<List<MensajeEntity>>(emptyList()); private set
    var texto by mutableStateOf(""); private set
    var error by mutableStateOf<String?>(null); private set
    var enviando by mutableStateOf(false); private set
    private var observacion: Job? = null
    init { viewModelScope.launch {
        usuario = cuenta.actual()
        if (usuario?.rol == "PACIENTE") seleccionar(usuario!!.email)
        else if (usuario != null) cuenta.db.centroDao().usuarios().collect { pacientes=it.filter { u -> u.rol=="PACIENTE" && u.activo } }
    } }
    fun escribir(valor: String) { texto=valor.take(2000) }
    fun seleccionar(email: String) {
        val u = usuario ?: return
        if (u.rol=="PACIENTE" && u.email!=email) return
        observacion?.cancel(); seleccionado=email; mensajes=emptyList(); error=null; texto=""
        observacion=viewModelScope.launch { repo.observar(email).collect { mensajes=it } }
    }
    fun enviar() {
        val paciente=seleccionado ?: return
        if(enviando || texto.isBlank()) return
        val contenido=texto; enviando=true
        viewModelScope.launch {
            try { repo.enviar(paciente,contenido); texto=""; error=null }
            catch(e:Exception){error=e.message ?: "No se pudo guardar el mensaje"}
            finally { enviando=false }
        }
    }
}
