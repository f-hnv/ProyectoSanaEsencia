package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sanaesencia.data.local.seed.SeedData
import com.example.sanaesencia.di.AppContainer


class ViewModelFactory(
    private val container: AppContainer,
    private val emailPaciente: String = container.emailPacienteActual ?: SeedData.EMAIL_DEMO
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(CatalogoViewModel::class.java) ->
            CatalogoViewModel(container.servicioRepository, container.especialistaRepository) as T

        modelClass.isAssignableFrom(AgendaViewModel::class.java) ->
            AgendaViewModel(container.citaRepository, container.servicioRepository, container.especialistaRepository, emailPaciente) as T

        modelClass.isAssignableFrom(SedeViewModel::class.java) ->
            SedeViewModel(container.locationHelper) as T

        else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
