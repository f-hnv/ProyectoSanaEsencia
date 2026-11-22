package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sanaesencia.data.local.seed.SeedData
import com.example.sanaesencia.di.AppContainer


class ViewModelFactory(
    private val container: AppContainer,
    private val emailPaciente: String = container.emailPacienteActual.orEmpty()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(CatalogoViewModel::class.java) ->
            CatalogoViewModel(container.catalogoRepository) as T

        modelClass.isAssignableFrom(AgendaViewModel::class.java) ->
            AgendaViewModel(container.citaRepository, container.servicioRepository, container.especialistaRepository,
                emailPaciente, container.catalogoRepository) as T

        modelClass.isAssignableFrom(SedeViewModel::class.java) ->
            SedeViewModel(container.locationHelper) as T

        modelClass.isAssignableFrom(RutasViewModel::class.java) ->
            RutasViewModel(container.rutasRepository) as T
        modelClass.isAssignableFrom(ReservaServidorViewModel::class.java) ->
            ReservaServidorViewModel(container) as T
        modelClass.isAssignableFrom(ListaReservasViewModel::class.java) ->
            ListaReservasViewModel(container.reservasRepository) as T
        modelClass.isAssignableFrom(AdministracionReservasViewModel::class.java) ->
            AdministracionReservasViewModel(container.reservasRepository) as T
        modelClass.isAssignableFrom(HorariosServidorViewModel::class.java) ->
            HorariosServidorViewModel(container.reservasRepository) as T

        else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
