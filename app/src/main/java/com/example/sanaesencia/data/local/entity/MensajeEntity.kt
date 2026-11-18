package com.example.sanaesencia.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
/** Conversación local paciente-centro. No representa entrega por Internet. */
@Entity(tableName = "mensajes", indices = [Index("pacienteEmail")])
data class MensajeEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pacienteEmail: String, val remitenteEmail: String, val remitenteNombre: String,
    val texto: String, val creadoEn: Long = System.currentTimeMillis())
