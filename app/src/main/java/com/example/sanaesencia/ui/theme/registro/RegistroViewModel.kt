package com.example.sanaesencia.ui.registro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel

class RegistroViewModel : ViewModel() {

    private val _nombre = mutableStateOf("")
    val nombre: State<String> = _nombre
    private val _nombreError = mutableStateOf<String?>(null)
    val nombreError: State<String?> = _nombreError

    private val _email = mutableStateOf("")
    val email: State<String> = _email
    private val _emailError = mutableStateOf<String?>(null)
    val emailError: State<String?> = _emailError

    private val _password = mutableStateOf("")
    val password: State<String> = _password
    private val _passwordError = mutableStateOf<String?>(null)
    val passwordError: State<String?> = _passwordError

    private val _confirmPassword = mutableStateOf("")
    val confirmPassword: State<String> = _confirmPassword
    private val _confirmPasswordError = mutableStateOf<String?>(null)
    val confirmPasswordError: State<String?> = _confirmPasswordError

    fun onNombreChange(newValue: String) {
        _nombre.value = newValue
        if (newValue.isBlank()) _nombreError.value = "El nombre es obligatorio" else _nombreError.value = null
    }

    fun onEmailChange(newValue: String) {
        _email.value = newValue
        if (newValue.isBlank()) {
            _emailError.value = "El correo es obligatorio"
        } else if (!newValue.endsWith("@duoc.cl") &&
            !newValue.endsWith("@profesor.duoc.cl") &&
            !newValue.endsWith("@gmail.com") &&
            !newValue.endsWith("@hotmail.com")) {
            _emailError.value = "Solo correos con @duoc.cl, @profesor.duoc.cl, @gmail.com y @hotmail.com"
        } else {
            _emailError.value = null
        }
    }

    fun onPasswordChange(newValue: String) {
        _password.value = newValue
        if (newValue.isBlank()) {
            _passwordError.value = "La contraseña es obligatoria"
        } else if (newValue.length < 6) {
            _passwordError.value = "Debe tener al menos 6 caracteres"
        } else {
            _passwordError.value = null
        }
        if (_confirmPassword.value.isNotEmpty()) onConfirmPasswordChange(_confirmPassword.value)
    }

    fun onConfirmPasswordChange(newValue: String) {
        _confirmPassword.value = newValue
        if (newValue != _password.value) {
            _confirmPasswordError.value = "Las contraseñas no coinciden"
        } else {
            _confirmPasswordError.value = null
        }
    }

    fun registrar(onSuccess: () -> Unit) {
        onNombreChange(_nombre.value)
        onEmailChange(_email.value)
        onPasswordChange(_password.value)
        onConfirmPasswordChange(_confirmPassword.value)

        if (_nombreError.value == null && _emailError.value == null &&
            _passwordError.value == null && _confirmPasswordError.value == null &&
            _nombre.value.isNotBlank() && _email.value.isNotBlank() &&
            _password.value.isNotBlank() && _confirmPassword.value.isNotBlank()
        ) {
            onSuccess()
        }
    }

}

