package com.example.sanaesencia.nativo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Resultado de pedir la ubicación. Cada caso lo traduce el ViewModel a un estado de UI. */
sealed interface UbicacionResultado {
    data class Exito(
        val latitud: Double,
        val longitud: Double,
        val precisionM: Float?   // radio de error en metros; null si el proveedor no lo informa
    ) : UbicacionResultado
    data object SinPermiso : UbicacionResultado
    data object UbicacionDesactivada : UbicacionResultado
    data object NoDisponible : UbicacionResultado   // timeout o el sistema no pudo calcular la posición
}

/**
 * Acceso al GPS del dispositivo. No muestra diálogos (eso es de la UI): solo verifica permisos,
 * valida que la ubicación esté activada y obtiene una lectura puntual de forma segura.
 */
class LocationHelper(context: Context) {

    // applicationContext: el helper vive en el AppContainer (toda la app); retener un Activity
    // provocaría una fuga de memoria.
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /** Se piden ambos juntos: desde Android 12 el usuario puede elegir ubicación precisa o aproximada. */
    val permisosRequeridos: Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /** true si el usuario concedió al menos uno de los dos permisos. */
    fun tienePermiso(): Boolean =
        concedido(Manifest.permission.ACCESS_FINE_LOCATION) ||
                concedido(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun tienePermisoPreciso(): Boolean =
        concedido(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun concedido(permiso: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permiso) == PackageManager.PERMISSION_GRANTED

    // Con permiso preciso se usa GPS; con solo aproximado (o GPS apagado) se usa la red (antenas/Wi-Fi).
    // Pedir el proveedor GPS sin permiso preciso lanzaría SecurityException.
    private fun elegirProveedor(): String? = when {
        tienePermisoPreciso() && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
            LocationManager.GPS_PROVIDER
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
            LocationManager.NETWORK_PROVIDER
        else -> null
    }

    suspend fun obtenerUbicacionActual(): UbicacionResultado {
        if (!tienePermiso()) return UbicacionResultado.SinPermiso
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            return UbicacionResultado.UbicacionDesactivada
        }
        val proveedor = elegirProveedor() ?: return UbicacionResultado.UbicacionDesactivada

        // Se intenta una lectura fresca con tiempo límite; si falla, se usa la última conocida.
        val ubicacion = withTimeoutOrNull(TIMEOUT_MS) { solicitarUbicacion(proveedor) }
            ?: ultimaConocida(proveedor)

        return ubicacion?.let {
            UbicacionResultado.Exito(
                latitud = it.latitude,
                longitud = it.longitude,
                precisionM = if (it.hasAccuracy()) it.accuracy else null
            )
        } ?: UbicacionResultado.NoDisponible
    }

    // Los permisos se verifican en obtenerUbicacionActual() antes de llegar aquí.
    @SuppressLint("MissingPermission")
    private suspend fun solicitarUbicacion(proveedor: String): Location? =
        suspendCancellableCoroutine { cont ->
            val senal = CancellationSignal()
            // Si la corrutina se cancela (ej. el ViewModel muere), se detiene la búsqueda GPS.
            cont.invokeOnCancellation { senal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    proveedor,
                    senal,
                    ContextCompat.getMainExecutor(appContext)
                ) { ubicacion: Location? ->
                    if (cont.isActive) cont.resume(ubicacion)
                }
            } catch (e: SecurityException) {
                // El permiso fue revocado justo entre la verificación y la solicitud.
                if (cont.isActive) cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private fun ultimaConocida(proveedor: String): Location? =
        try {
            locationManager.getLastKnownLocation(proveedor)
        } catch (e: SecurityException) {
            null
        }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}