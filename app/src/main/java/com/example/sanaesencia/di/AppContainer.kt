package com.example.sanaesencia.di

import android.content.Context
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.repository.CitaRepository
import com.example.sanaesencia.data.repository.EspecialistaRepository
import com.example.sanaesencia.data.repository.ServicioRepository
import com.example.sanaesencia.nativo.LocationHelper

// Inyección manual de dependencias: un único lugar donde se construyen los objetos
// compartidos de la app (BD y repositorios), sin librerías externas como Hilt.
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)

    val servicioRepository = ServicioRepository(database.servicioDao())
    val especialistaRepository = EspecialistaRepository(database.especialistaDao())
    val citaRepository = CitaRepository(database)

    val locationHelper = LocationHelper(context)
}