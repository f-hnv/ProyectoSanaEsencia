package com.example.sanaesencia.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.sanaesencia.data.local.entity.CitaEntity
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import com.example.sanaesencia.data.local.entity.ServicioEntity

data class CitaConDetalle(
    @Embedded val cita: CitaEntity,
    @Relation(parentColumn = "servicioId", entityColumn = "id")
    val servicio: ServicioEntity,
    @Relation(parentColumn = "especialistaId", entityColumn = "id")
    val especialista: EspecialistaEntity
)