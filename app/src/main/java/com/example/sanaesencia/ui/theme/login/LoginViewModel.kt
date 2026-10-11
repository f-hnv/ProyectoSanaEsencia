package com.example.sanaesencia.ui.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    private val _email = mutableStateOf("")
    val email: State<String> = _email

    private val _emailError = mutableStateOf<String?>(null)
    val emailError: State<String?> = _emailError

    private val _password = mutableStateOf("")
    val password: State<String> = _password

    private val _passwordError = mutableStateOf<String?>(null)
    val passwordError: State<String?> = _passwordError

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
            _emailError.value = "Solo correos con @duoc.cl, @profesor.duoc.cl, @gmail.com y @hotmail.com"
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

    fun login(onSuccess: () -> Unit) {
        validateEmail(_email.value)
        validatePassword(_password.value)

        if (_emailError.value == null && _passwordError.value == null && _email.value.isNotBlank() && _password.value.isNotBlank()) {
            onSuccess()
        }
    }
}

