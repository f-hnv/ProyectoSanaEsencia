package com.example.sanaesencia.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Copia del perfil. Los campos hash/sal históricos quedan vacíos al iniciar sesión en el servidor. */
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
