package com.example.sanaesencia.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/** Un ID de MySQL nunca reemplaza por coincidencia un registro antiguo del dispositivo. */
@Entity(tableName="catalogo_vinculos", primaryKeys=["origen","tipo","servidorId"],
    indices=[Index(value=["origen","tipo","localId"], unique=true)])
data class CatalogoVinculoEntity(val origen:String,val tipo:String,val servidorId:Long,val localId:Long)

@Entity(tableName="catalogo_sincronizacion", primaryKeys=["origen"])
data class CatalogoSincronizacionEntity(val origen:String,val actualizadoEn:Long)
