package com.example.sanaesencia

import android.app.Application
import com.example.sanaesencia.data.local.seed.CatalogoCentro
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

        // Carga el catálogo público sin crear cuentas ni reservas ficticias.
        appScope.launch {
            CatalogoCentro.instalar(container.database)
            com.example.sanaesencia.data.repository.CuentaRepository(this@SanaesenciaApp).prepararDemostracion()
        }




    }
}
