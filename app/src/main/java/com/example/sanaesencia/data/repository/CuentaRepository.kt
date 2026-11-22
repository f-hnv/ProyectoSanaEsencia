package com.example.sanaesencia.data.repository

import android.content.Context
import android.net.Uri
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.UsuarioEntity
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.utils.ValidacionCuenta
import com.example.sanaesencia.utils.ValidacionRegistro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map

/** El servidor valida identidad y rol. Room conserva una copia del perfil y recursos locales. */
class CuentaRepository(context: Context, private val api: UsuariosApi =
    (context.applicationContext as? com.example.sanaesencia.SanaesenciaApp)?.container?.usuariosApi ?: ConexionBackend.usuarios) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("sanaesencia_prefs", Context.MODE_PRIVATE)
    val db = AppDatabase.getInstance(appContext)
    private val dao = db.centroDao()
    val emailActual: String get() = SesionBackend.actual()?.usuario?.correo.orEmpty()
    fun observarActual() = dao.observarUsuario(emailActual).map { cache -> perfilDeSesion(cache) }
    private fun perfilDeSesion(cache: UsuarioEntity?): UsuarioEntity? {
        val u = SesionBackend.actual()?.usuario ?: return null
        return cache?.takeIf { it.email == u.correo && u.activo }?.copy(
            nombre="${u.nombre} ${u.apellido}".trim(), rol=u.rol, especialistaId=u.especialistaId, activo=u.activo)
    }
    suspend fun actual(): UsuarioEntity? = perfilDeSesion(dao.usuario(emailActual))
    fun salir() {
        SesionBackend.cerrar()
        prefs.edit().remove("email_sesion").remove("password_registrada").apply()
    }
    fun limpiarCredencialesAnteriores() { prefs.edit().remove("password_registrada").remove("email_sesion").apply() }
    suspend fun registrar(nombre: String, email: String, clave: String, apellido: String, run: String) {
        require(ValidacionRegistro.nombre(nombre) == null) { "Revisa el nombre" }
        require(ValidacionRegistro.nombre(apellido) == null) { "El apellido es obligatorio" }
        require(ValidacionRegistro.run(run.trim()) == null) { ValidacionRegistro.run(run.trim()).orEmpty() }
        require(ValidacionCuenta.correo(email) == null) { "Ingresa un correo válido" }
        require(ValidacionRegistro.clave(clave) == null) { ValidacionRegistro.clave(clave).orEmpty() }
        llamadaBackend { api.registrar(RegistroRequest(nombre.trim(), apellido.trim(), run.trim().uppercase(), email.trim().lowercase(), clave)) }
    }
    suspend fun entrar(email: String, clave: String): UsuarioEntity = withContext(Dispatchers.IO) {
        salir()
        val u = llamadaBackend { api.login(LoginRequest(email.trim().lowercase(), clave)) }
        SesionBackend.validar(u)
        require(u.correo.equals(email.trim(), true)) { "El servidor devolvió una cuenta no válida" }
        val cache = guardarCache(u)
        SesionBackend.abrir(u, clave)
        prefs.edit().putString("nombre_registrado",cache.nombre).apply()
        cache
    }
    private suspend fun guardarCache(u: UsuarioRemoto): UsuarioEntity {
        val anterior = dao.usuario(u.correo)
        val cache = UsuarioEntity(email=u.correo, nombre="${u.nombre} ${u.apellido}".trim(), hash="", sal="",
            rol=u.rol, especialistaId=u.especialistaId, activo=u.activo,
            telefono=anterior?.telefono.orEmpty(), fotoUri=anterior?.fotoUri)
        if (anterior == null) dao.insertarUsuario(cache) else dao.actualizarUsuario(cache)
        return cache
    }
    suspend fun editarPerfil(nombre: String, telefono: String, apellido: String = SesionBackend.actual()?.usuario?.apellido.orEmpty()) = withContext(Dispatchers.IO) {
        val sesion = requireNotNull(SesionBackend.actual()) { "Inicia sesión para editar tu perfil" }
        require(ValidacionRegistro.nombre(nombre) == null && ValidacionRegistro.nombre(apellido) == null) { "Revisa nombre y apellido" }
        require(ValidacionCuenta.telefono(telefono) == null) { "Ingresa un teléfono válido" }
        val remoto = llamadaBackend { api.editar(sesion.usuario.id, sesion.autorizacion, PerfilRequest(nombre.trim(), apellido.trim())) }
        SesionBackend.validar(remoto)
        SesionBackend.actualizar(remoto)
        val cache = guardarCache(remoto)
        dao.actualizarUsuario(cache.copy(telefono=telefono.trim()))
        prefs.edit().putString("nombre_registrado",cache.nombre).apply()
    }
    suspend fun guardarFoto(uri: Uri) = withContext(Dispatchers.IO) {
        val u = actual() ?: error("Inicia sesión")
        val carpeta = java.io.File(appContext.filesDir,"perfiles").apply { mkdirs() }
        val archivo = java.io.File(carpeta,"${java.util.UUID.randomUUID()}.jpg")
        appContext.contentResolver.openInputStream(uri).use { entrada ->
            requireNotNull(entrada) { "No se pudo leer la imagen" }
            archivo.outputStream().use { entrada.copyTo(it) }
        }
        dao.actualizarUsuario(u.copy(fotoUri=Uri.fromFile(archivo).toString()))
    }
    suspend fun buscarUsuario(id: Long): UsuarioRemoto {
        val sesion = requireNotNull(SesionBackend.actual()) { "Inicia sesión" }
        require(sesion.usuario.rol == "ADMIN") { "Solo administración puede consultar cuentas" }
        return llamadaBackend { api.obtener(id, sesion.autorizacion) }
    }
    suspend fun asignarRol(id: Long, rol: String, especialistaId: Long?): UsuarioRemoto {
        val sesion = requireNotNull(SesionBackend.actual()) { "Inicia sesión" }
        require(sesion.usuario.rol == "ADMIN") { "Solo administración puede cambiar roles" }
        require(rol in setOf("PACIENTE", "ESPECIALISTA")) { "Selecciona paciente o especialista" }
        if (rol == "ESPECIALISTA") {
            val perfiles = (appContext as? com.example.sanaesencia.SanaesenciaApp)?.container?.perfilesApi ?: ConexionBackend.perfiles
            val perfil = llamadaBackend { perfiles.obtener(requireNotNull(especialistaId) { "Indica el perfil profesional" }, sesion.autorizacion) }
            require(perfil.activo && perfil.id == especialistaId) { "El perfil profesional no está activo" }
        }
        return llamadaBackend { api.cambiarRol(id, sesion.autorizacion, RolRequest(rol, if (rol == "ESPECIALISTA") especialistaId else null)) }
    }
}
