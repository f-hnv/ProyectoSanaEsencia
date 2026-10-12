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

        // --- PRUEBA TEMPORAL ---
        appScope.launch {
            // Espera 2 segundos para asegurar que Room termine de insertar los datos
            kotlinx.coroutines.delay(2000)

            // Prueba 1: GPS Nativo
            val gps = container.locationHelper.obtenerUbicacionActual()
            android.util.Log.e("PRUEBA_SANAESENCIA", "Resultado GPS: $gps")

            // Prueba 2: Calculadora de Riesgo de Deserción
            container.alertasRepository.observarAlertas().collect { alertas ->
                android.util.Log.e("PRUEBA_SANAESENCIA", "=== PACIENTES EN RIESGO ===")
                alertas.forEach { alerta ->
                    android.util.Log.e("PRUEBA_SANAESENCIA", "Paciente: ${alerta.emailPaciente} | Nivel: ${alerta.nivel} | Puntaje: ${alerta.puntaje}")
                    android.util.Log.e("PRUEBA_SANAESENCIA", "Motivos: ${alerta.motivos}")
                }
            }
        }
    }
}