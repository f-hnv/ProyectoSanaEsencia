package com.example.sanaesencia

import android.app.Application
import com.example.sanaesencia.data.local.seed.SeedData
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

        // Los datos demo se insertan en segundo plano.
        appScope.launch { SeedData.poblarSiVacia(container.database) }




    }
}