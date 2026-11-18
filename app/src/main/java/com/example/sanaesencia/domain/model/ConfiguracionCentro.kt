package com.example.sanaesencia.domain.model
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Solo la distancia calculada vive en memoria; no persistimos coordenadas del paciente. */
object ConfiguracionCentro {
    private val _umbral=MutableStateFlow(20f)
    val umbralKm=_umbral.asStateFlow()
    val distanciaMetros=MutableStateFlow<Double?>(null)
    fun cargar(context:Context){_umbral.value=context.getSharedPreferences("centro_config",0).getFloat("umbral_km",20f)}
    fun configurar(context:Context,km:Float){require(km in 1f..1000f);context.getSharedPreferences("centro_config",0).edit().putFloat("umbral_km",km).apply();_umbral.value=km}
}
