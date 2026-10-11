package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sanaesencia.data.local.seed.SeedData
import com.example.sanaesencia.di.AppContainer

/**
 * Construye los ViewModels inyectándoles las dependencias del AppContainer.
 * Android no sabe crear ViewModels con parámetros, por eso necesita esta fábrica.
 *
 * USO desde Compose:
 *   val container = (LocalContext.current.applicationContext as SanaesenciaApp).container
 *   val factory = remember { ViewModelFactory(container) }
 *   val catalogoVm: CatalogoViewModel = viewModel(factory = factory)
 *   val agendaVm: AgendaViewModel = viewModel(factory = factory)
 *   val sedeVm: SedeViewModel = viewModel(factory = factory)
 *
 * TODO(login): cuando exista la sesión, pasar aquí el email del usuario autenticado.
 */
class ViewModelFactory(
    private val container: AppContainer,
    private val emailPaciente: String = SeedData.EMAIL_DEMO
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(CatalogoViewModel::class.java) ->
            CatalogoViewModel(container.servicioRepository, container.especialistaRepository) as T

        modelClass.isAssignableFrom(AgendaViewModel::class.java) ->
            AgendaViewModel(container.citaRepository, container.servicioRepository, emailPaciente) as T

        modelClass.isAssignableFrom(SedeViewModel::class.java) ->
            SedeViewModel(container.locationHelper) as T

        else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
