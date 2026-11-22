package com.example.sanaesencia.utils

object ValidacionRegistro {
    fun nombre(valor: String): String? = if (valor.trim().length !in 1..100) "Ingresa entre 1 y 100 caracteres" else null
    fun clave(valor: String): String? = if (valor.length !in 8..64 || valor.toByteArray(Charsets.UTF_8).size > 72)
        "La contraseña debe tener entre 8 y 64 caracteres (máximo 72 bytes)" else null
    fun run(valor: String): String? {
        if (!Regex("^[1-9][0-9]{6,7}-[0-9kK]$").matches(valor)) return "Usa un RUN sin puntos y con guion: 12345678-5"
        val cuerpo = valor.substringBefore('-')
        val suma = cuerpo.reversed().mapIndexed { i, c -> c.digitToInt() * (2 + i % 6) }.sum()
        val digito = when (val resto = 11 - suma % 11) { 11 -> '0'; 10 -> 'K'; else -> ('0'.code + resto).toChar() }
        return if (valor.last().uppercaseChar() == digito) null else "El dígito verificador del RUN no es válido"
    }
}
