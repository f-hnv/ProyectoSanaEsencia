package com.example.sanaesencia.data.local.entity
import androidx.room.Entity
@Entity(tableName = "especialista_servicio", primaryKeys = ["especialistaId", "servicioId"])
data class EspecialistaServicioEntity(val especialistaId: Long, val servicioId: Long)
