package com.example.sanaesencia.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Gestiona la validación del acceso, la sesión local y el estado de la bienvenida. */
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
        if (emailInput.isBlank()) {
            _emailError.value = "El correo es obligatorio"
        } else if (!emailInput.endsWith("@duoc.cl") &&
            !emailInput.endsWith("@profesor.duoc.cl") &&
            !emailInput.endsWith("@gmail.com") &&
            !emailInput.endsWith("@hotmail.com")) {
            _emailError.value = "Solo correos permitidos (@duoc.cl, @gmail.com, etc.)"
        } else {
            _emailError.value = null
        }
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
                // El acceso local compara las credenciales con la cuenta guardada en el registro.
                val sharedPref = context.getSharedPreferences("sanaesencia_prefs", Context.MODE_PRIVATE)
                val emailGuardado = sharedPref.getString("email_registrado", null)
                val passwordGuardada = sharedPref.getString("password_registrada", null)

                // Solo una coincidencia de correo y contraseña crea la sesión del paciente.
                if (emailGuardado == _email.value && passwordGuardada == _password.value) {
                    sharedPref.edit().putString("email_sesion", emailGuardado).apply()
                    // La confirmación visual aparece solo después de verificar las credenciales.
                    _accesoConcedido.value = true
                    delay(900)
                    _cargando.value = false
                    onSuccess()
                } else if (emailGuardado == null) {
                    _cargando.value = false
                    _emailError.value = "No existe una cuenta. ¡Regístrate primero!"
                } else {
                    _cargando.value = false
                    _passwordError.value = "Credenciales incorrectas"
                }
            }
        }
    }
}
