package com.example.sanaesencia.data.remote

import com.example.sanaesencia.BuildConfig
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ConexionBackend {
    private val cliente = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()
    fun crearUsuarios(url: String, clienteHttp: OkHttpClient = cliente): UsuariosApi =
        retrofit(url, clienteHttp).create(UsuariosApi::class.java)
    fun crearCatalogo(url: String, clienteHttp: OkHttpClient = cliente): CatalogoApi =
        retrofit(url, clienteHttp).create(CatalogoApi::class.java)
    val catalogo: CatalogoApi by lazy { crearCatalogo(BuildConfig.CATALOGO_API_URL) }
    fun crearReservas(url:String,clienteHttp:OkHttpClient=cliente):ReservasApi =
        retrofit(url,clienteHttp.newBuilder().retryOnConnectionFailure(false).build()).create(ReservasApi::class.java)
    val reservas:ReservasApi by lazy { crearReservas(BuildConfig.RESERVAS_API_URL) }
    private fun retrofit(url: String, clienteHttp: OkHttpClient) = Retrofit.Builder().baseUrl(url)
        .client(clienteHttp).addConverterFactory(GsonConverterFactory.create()).build()
    val usuarios: UsuariosApi by lazy { crearUsuarios(BuildConfig.USUARIOS_API_URL) }
    val perfiles: PerfilesApi by lazy {
        retrofit(BuildConfig.CATALOGO_API_URL, cliente).create(PerfilesApi::class.java)
    }
}

class ErrorBackend(val estado: Int?, override val message: String): Exception(message)

/** Presenta errores de la API sin mostrar trazas, HTML ni credenciales. */
suspend fun <T> llamadaBackend(bloque: suspend () -> T): T = try { bloque() }
catch (e: CancellationException) { throw e }
catch (e: HttpException) {
    val mensajeApi = runCatching {
        JsonParser.parseString(e.response()?.errorBody()?.string().orEmpty()).asJsonObject
            .get("mensaje")?.asString?.takeIf { it.length <= 250 }
    }.getOrNull()
    val mensaje = when (e.code()) {
        401 -> "Correo o contraseña incorrectos, o cuenta desactivada"
        403 -> "Tu cuenta no tiene permiso para realizar esta acción"
        404 -> "No se encontró la cuenta o el perfil solicitado"
        409 -> mensajeApi ?: "El correo o RUN ya está registrado"
        400 -> mensajeApi ?: "Revisa los datos ingresados"
        else -> "El servidor no está disponible. Inténtalo nuevamente"
    }
    throw ErrorBackend(e.code(), mensaje)
} catch (e: IOException) {
    throw ErrorBackend(null, "No se pudo conectar con el servidor. Revisa la conexión e inténtalo nuevamente")
} catch (e: com.google.gson.JsonParseException) {
    throw ErrorBackend(null, "El servidor devolvió una respuesta no válida. Inténtalo nuevamente")
} catch (e: IllegalStateException) {
    // Algunos TypeAdapter de Gson lanzan IllegalStateException al recibir un tipo JSON distinto.
    if(generateSequence<Throwable>(e) { it.cause }.take(8).any { causa ->
        causa.stackTrace.any { it.className.startsWith("com.google.gson.") }
    }) {
        throw ErrorBackend(null, "El servidor devolvió una respuesta no válida. Inténtalo nuevamente")
    }
    throw e
}
