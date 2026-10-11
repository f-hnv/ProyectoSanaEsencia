package com.example.sanaesencia.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sanaesencia.domain.model.EstadoCita
import com.example.sanaesencia.domain.model.Modalidad

@Entity(
    tableName = "citas",
    foreignKeys = [
        ForeignKey(
            entity = ServicioEntity::class,
            parentColumns = ["id"],
            childColumns = ["servicioId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = EspecialistaEntity::class,
            parentColumns = ["id"],
            childColumns = ["especialistaId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("servicioId"),
        Index("especialistaId"),
        Index("emailPaciente"),
        Index(value = ["especialistaId", "fechaHoraInicio"], unique = true)
    ]
)
data class CitaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emailPaciente: String,
    val servicioId: Long,
    val especialistaId: Long,
    val fechaHoraInicio: Long,      // epoch millis
    val duracionMin: Int,           // copia del servicio al agendar
    val modalidad: Modalidad,
    val estado: EstadoCita = EstadoCita.PROGRAMADA,
    val notas: String = "",
    val creadaEn: Long = System.currentTimeMillis(),
    val actualizadaEn: Long = System.currentTimeMillis()
)