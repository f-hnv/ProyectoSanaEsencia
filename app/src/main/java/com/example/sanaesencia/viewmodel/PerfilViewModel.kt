package com.example.sanaesencia.viewmodel
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.example.sanaesencia.data.repository.CuentaRepository
import com.example.sanaesencia.data.local.entity.UsuarioEntity
import kotlinx.coroutines.launch

class PerfilViewModel(app: Application) : AndroidViewModel(app) {
    private val cuenta = CuentaRepository(app)
    private val _fotoPerfilUri = mutableStateOf<Uri?>(null)
    val fotoPerfilUri: State<Uri?> = _fotoPerfilUri
    var perfil by mutableStateOf<UsuarioEntity?>(null); private set
    var mensaje by mutableStateOf<String?>(null); private set
    var guardando by mutableStateOf(false); private set
    init { viewModelScope.launch { cuenta.observarActual().collect {
        perfil = it; _fotoPerfilUri.value = it?.fotoUri?.let(Uri::parse)
    } } }
    fun actualizarFotoPerfil(uri: Uri?) { if (uri != null) ejecutar { cuenta.guardarFoto(uri) } }
    fun guardar(nombre: String, telefono: String) = ejecutar { cuenta.editarPerfil(nombre,telefono) }
    fun cambiarClave(actual: String, nueva: String, confirmacion: String) = ejecutar {
        cuenta.cambiarClave(actual,nueva,confirmacion)
    }
    private fun ejecutar(bloque: suspend () -> Unit) {
        if (guardando) return
        guardando = true
        viewModelScope.launch {
            try { bloque(); mensaje = "Cambios guardados" }
            catch (e: Exception) { mensaje = e.message ?: "No se pudieron guardar los cambios" }
            finally { guardando = false }
        }
    }
}
