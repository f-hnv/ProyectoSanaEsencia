package com.example.sanaesencia.data.repository

import com.example.sanaesencia.data.remote.RutasApi
import com.example.sanaesencia.data.remote.OrigenRuta
import com.example.sanaesencia.data.remote.SesionBackend
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import kotlin.math.ceil

data class RecorridoSede(val metros: Double, val segundos: Double) {
    val distanciaTexto: String get() = if (metros < 1000) "${ceil(metros).toInt()} m"
        else String.format(Locale.forLanguageTag("es-CL"), "%.1f km", metros / 1000)
    val duracionTexto: String get() {
        val minutos = ceil(segundos / 60).toInt().coerceAtLeast(1)
        return if (minutos < 60) "$minutos min" else "${minutos / 60} h ${minutos % 60} min"
    }
}

class ErrorRuta(override val message: String): Exception(message)

class RutasRepository(private val api:RutasApi,
    private val autorizacion:()->String?={SesionBackend.actual()?.autorizacion}) {
    suspend fun consultar(latitud: Double, longitud: Double): RecorridoSede {
        if (!latitud.isFinite() || !longitud.isFinite() || latitud !in -90.0..90.0 || longitud !in -180.0..180.0)
            throw ErrorRuta("La ubicación no es válida. Vuelve a consultar tu ubicación.")
        val credencial = autorizacion()?.trim().orEmpty()
        if (credencial.isBlank()) throw ErrorRuta("Inicia sesión para consultar el recorrido. También puedes abrir la sede en mapas.")
        try {
            val respuesta = api.ruta(OrigenRuta(latitud,longitud), credencial)
            if(autorizacion()?.trim()!=credencial)throw CancellationException("La sesión cambió durante la consulta")
            if (!respuesta.isSuccessful) throw HttpException(respuesta)
            val ruta = respuesta.body()?.route
            val metros = ruta?.distance
            val segundos = ruta?.duration
            if (metros == null || segundos == null || !metros.isFinite() || !segundos.isFinite() || metros < 0 || segundos < 0)
                throw ErrorRuta("No se encontró un recorrido en auto para esta ubicación. Puedes abrir la sede en mapas.")
            return RecorridoSede(metros, segundos)
        } catch (e: CancellationException) { throw e }
        catch (e: ErrorRuta) { throw e }
        catch (e: HttpException) {
            throw ErrorRuta(when(e.code()) {
                401, 403 -> "Inicia sesión nuevamente para consultar el recorrido. Puedes abrir la sede en mapas."
                429 -> "Se alcanzó el límite de consultas de recorridos. Inténtalo más tarde."
                else -> "No se pudo consultar el recorrido. Inténtalo nuevamente."
            })
        } catch (e: IOException) {
            throw ErrorRuta("No se pudo conectar con el servicio de recorridos. Revisa tu conexión.")
        } catch (e: com.google.gson.JsonParseException) {
            throw ErrorRuta("El servicio de recorridos devolvió una respuesta no válida.")
        } catch (e: IllegalStateException) {
            throw ErrorRuta("No se pudo interpretar el recorrido. Inténtalo nuevamente.")
        }
    }
}
