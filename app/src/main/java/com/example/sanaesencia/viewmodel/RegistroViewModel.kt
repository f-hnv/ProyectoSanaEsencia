package com.example.sanaesencia.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel

/** Valida el formulario y registra al paciente en el servidor. */
class RegistroViewModel : ViewModel() {

    private val _nombre = mutableStateOf("")
    val nombre: State<String> = _nombre
    private val _nombreError = mutableStateOf<String?>(null)
    val nombreError: State<String?> = _nombreError
    val apellido = mutableStateOf("")
    val apellidoError = mutableStateOf<String?>(null)
    val run = mutableStateOf("")
    val runError = mutableStateOf<String?>(null)
    val cargando = mutableStateOf(false)
    val errorGeneral = mutableStateOf<String?>(null)

    fun onApellidoChange(valor: String) {
        apellido.value = valor
        apellidoError.value = com.example.sanaesencia.utils.ValidacionRegistro.nombre(valor)
    }
    fun onRunChange(valor: String) {
        run.value = valor.uppercase()
        runError.value = com.example.sanaesencia.utils.ValidacionRegistro.run(run.value)
    }

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
        _nombreError.value = com.example.sanaesencia.utils.ValidacionRegistro.nombre(newValue)
    }

    fun onEmailChange(newValue: String) {
        _email.value = newValue
        _emailError.value = com.example.sanaesencia.utils.ValidacionCuenta.correo(newValue)
    }

    fun onPasswordChange(newValue: String) {
        _password.value = newValue
        _passwordError.value = com.example.sanaesencia.utils.ValidacionRegistro.clave(newValue)
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

    fun registrar(context: Context, onSuccess: () -> Unit) {
        if (cargando.value) return
        errorGeneral.value = null
        onNombreChange(_nombre.value)
        onApellidoChange(apellido.value)
        onRunChange(run.value)
        onEmailChange(_email.value)
        onPasswordChange(_password.value)
        onConfirmPasswordChange(_confirmPassword.value)

        if (apellidoError.value == null && runError.value == null && _nombreError.value == null && _emailError.value == null &&
            _passwordError.value == null && _confirmPasswordError.value == null &&
            _nombre.value.isNotBlank() && _email.value.isNotBlank() &&
            _password.value.isNotBlank() && _confirmPassword.value.isNotBlank()
        ) {
            cargando.value = true
            viewModelScope.launch {
                try {
                    com.example.sanaesencia.data.repository.CuentaRepository(context)
                        .registrar(_nombre.value,_email.value,_password.value,apellido.value,run.value)
                    _password.value = ""
                    _confirmPassword.value = ""
                    onSuccess()
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { errorGeneral.value = e.message ?: "No se pudo crear la cuenta" }
                finally { cargando.value = false }
            }
        }
    }
}

