package com.example.sanaesencia.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.remote.UsuarioRemoto
import com.example.sanaesencia.data.repository.CuentaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class UsuariosAdminViewModel(app: Application): AndroidViewModel(app) {
    private val repo = CuentaRepository(app)
    var cuenta by mutableStateOf<UsuarioRemoto?>(null); private set
    var mensaje by mutableStateOf<String?>(null); private set
    var ocupado by mutableStateOf(false); private set
    fun buscar(id: String) = operar {
        cuenta = null
        cuenta = repo.buscarUsuario(requireNotNull(id.toLongOrNull()?.takeIf { it > 0 }) { "Ingresa un número de cuenta válido" })
    }
    fun guardar(rol: String, perfilId: String) = operar {
        val u = requireNotNull(cuenta) { "Busca una cuenta" }
        val id = if (rol == "ESPECIALISTA") requireNotNull(perfilId.toLongOrNull()?.takeIf { it > 0 }) { "Ingresa el número del perfil profesional" } else null
        cuenta = repo.asignarRol(u.id, rol, id)
        mensaje = "Rol guardado en el servidor. Se aplicará al próximo inicio de sesión de esa cuenta."
    }
    private fun operar(accion: suspend () -> Unit) {
        if (ocupado) return
        ocupado = true; mensaje = null
        viewModelScope.launch {
            try { accion() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mensaje = e.message ?: "No se pudo completar la operación" }
            finally { ocupado = false }
        }
    }
}
