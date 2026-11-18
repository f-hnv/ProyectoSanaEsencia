package com.example.sanaesencia.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "boxes")
data class BoxEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String, val activo: Boolean = true)
