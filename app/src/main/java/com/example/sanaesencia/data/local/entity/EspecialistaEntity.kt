package com.example.sanaesencia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "especialistas")
data class EspecialistaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val especialidad: String,
    val descripcion: String = "",
    val fotoUri: String? = null,
    val activo: Boolean = true
)