package com.example.sanaesencia.data.local.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servicios")
data class ServicioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val descripcion: String,
    val categoria: String,          // ej. "Psicoterapia", "Reiki", "Mindfulness"
    val duracionMin: Int,
    val precioClp: Int,
    val permiteOnline: Boolean = true,
    val imagenUri: String? = null,
    val activo: Boolean = true
)