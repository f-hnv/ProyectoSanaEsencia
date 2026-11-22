package com.example.sanaesencia.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Valida el acceso contra usuarios y mantiene el estado de la bienvenida. */
class LoginViewModel : ViewModel() {

    private val _email = mutableStateOf("")
    val email: State<String> = _email

    private val _emailError = mutableStateOf<String?>(null)
    val emailError: State<String?> = _emailError

    private val _password = mutableStateOf("")
    val password: State<String> = _password

    private val _passwordError = mutableStateOf<String?>(null)
    val passwordError: State<String?> = _passwordError

    private val _cargando = mutableStateOf(false)
    val cargando: State<Boolean> = _cargando
    private val _accesoConcedido = mutableStateOf(false)
    val accesoConcedido: State<Boolean> = _accesoConcedido

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        validateEmail(newEmail)
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        validatePassword(newPassword)
    }

    private fun validateEmail(emailInput: String) {
        _emailError.value = com.example.sanaesencia.utils.ValidacionCuenta.correo(emailInput)
    }

    private fun validatePassword(passwordInput: String) {
        if (passwordInput.isBlank()) {
            _passwordError.value = "La contraseña es obligatoria"
        } else if (passwordInput.length < 6) {
            _passwordError.value = "Debe tener al menos 6 caracteres"
        } else {
            _passwordError.value = null
        }
    }

    fun login(context: Context, onSuccess: () -> Unit) {
        validateEmail(_email.value)
        validatePassword(_password.value)

        if (_emailError.value == null && _passwordError.value == null &&
            _email.value.isNotBlank() && _password.value.isNotBlank() &&
            !_cargando.value) {

            _cargando.value = true
            _accesoConcedido.value = false

            viewModelScope.launch {
                try {
                    com.example.sanaesencia.data.repository.CuentaRepository(context).entrar(_email.value,_password.value)
                    _password.value = ""
                    _accesoConcedido.value = true
                    delay(900)
                    _cargando.value = false
                    onSuccess()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _cargando.value = false
                    _passwordError.value = e.message ?: "No se pudo iniciar sesión. Reintenta."
                }
            }
        }
    }
}
