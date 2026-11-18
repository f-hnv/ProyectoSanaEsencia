package com.example.sanaesencia.utils
import android.util.Patterns
object ValidacionCuenta {
    fun correo(valor: String): String? = when {
        valor.isBlank() -> "El correo es obligatorio"
        !Patterns.EMAIL_ADDRESS.matcher(valor.trim()).matches() -> "Ingresa un correo válido"
        else -> null
    }
    fun clave(valor: String): String? = when {
        valor.isBlank() -> "La contraseña es obligatoria"
        valor.length < 6 -> "Debe tener al menos 6 caracteres"
        else -> null
    }
    fun telefono(valor: String): String? = if (valor.isBlank() ||
        (valor.filter { it.isDigit() }.length in 8..15 && valor.all { it.isDigit() || it in "+ -()" })) null
        else "Ingresa un teléfono válido (8 a 15 dígitos)"
}
