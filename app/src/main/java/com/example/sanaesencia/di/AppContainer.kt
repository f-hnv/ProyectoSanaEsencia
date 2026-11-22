package com.example.sanaesencia.di

import android.content.Context
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.repository.AlertasRepository
import com.example.sanaesencia.data.repository.CitaRepository
import com.example.sanaesencia.data.repository.EspecialistaRepository
import com.example.sanaesencia.data.repository.ServicioRepository
import com.example.sanaesencia.nativo.LocationHelper

// Inyección manual de dependencias: un único lugar donde se construyen los objetos
// compartidos de la app (BD, repositorios y recursos nativos), sin librerías externas como Hilt.
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    var usuariosApi: com.example.sanaesencia.data.remote.UsuariosApi = com.example.sanaesencia.data.remote.ConexionBackend.usuarios
    var perfilesApi: com.example.sanaesencia.data.remote.PerfilesApi = com.example.sanaesencia.data.remote.ConexionBackend.perfiles
    val emailPacienteActual: String?
        get() = com.example.sanaesencia.data.remote.SesionBackend.actual()?.usuario?.correo
    val database: AppDatabase = AppDatabase.getInstance(context)
    var catalogoApi = com.example.sanaesencia.data.remote.ConexionBackend.catalogo
    var catalogoRepository = com.example.sanaesencia.data.repository.CatalogoRepository(database,catalogoApi)
    var reservasRepository = com.example.sanaesencia.data.repository.ReservasRepository(
        com.example.sanaesencia.data.remote.ConexionBackend.reservas)

    val servicioRepository = ServicioRepository(database.servicioDao()) { catalogoRepository }
    val especialistaRepository = EspecialistaRepository(database.especialistaDao()) { catalogoRepository }
    val citaRepository = CitaRepository(database)

    val alertasRepository = AlertasRepository(database.citaDao())

    val locationHelper = LocationHelper(context)
    val rutasRepository = com.example.sanaesencia.data.repository.RutasRepository(
        com.example.sanaesencia.data.remote.ConexionTrueWay.api) { com.example.sanaesencia.BuildConfig.TRUEWAY_API_KEY }
}
