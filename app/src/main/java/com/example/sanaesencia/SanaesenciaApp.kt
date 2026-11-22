package com.example.sanaesencia

import android.app.Application
import com.example.sanaesencia.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SanaesenciaApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        com.example.sanaesencia.domain.model.ConfiguracionCentro.cargar(this)

        // Retira credenciales antiguas. El catálogo se obtiene de la API al abrir su pantalla.
        appScope.launch {
            com.example.sanaesencia.data.repository.CuentaRepository(this@SanaesenciaApp).limpiarCredencialesAnteriores()
        }




    }
}
