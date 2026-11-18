package com.example.sanaesencia.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cuenta local de demostración. La contraseña se verifica con PBKDF2 y sal individual. */
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val email: String,
    val nombre: String,
    val telefono: String = "",
    val hash: String,
    val sal: String,
    val rol: String = "PACIENTE",
    val especialistaId: Long? = null,
    val fotoUri: String? = null,
    val activo: Boolean = true
)
