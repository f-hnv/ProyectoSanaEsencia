package com.example.sanaesencia.data.repository

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.utils.ValidacionCuenta
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Autenticación local académica. No es una sesión de servidor ni sincroniza dispositivos. */
class CuentaRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("sanaesencia_prefs", Context.MODE_PRIVATE)
    val db = AppDatabase.getInstance(appContext)
    private val dao = db.centroDao()
    val emailActual: String get() = prefs.getString("email_sesion", "").orEmpty()
    fun observarActual() = dao.observarUsuario(emailActual)
    suspend fun actual(): UsuarioEntity? = dao.usuario(emailActual)?.takeIf { it.activo }
    fun salir() { prefs.edit().remove("email_sesion").apply() }

    private fun nuevaCuenta(email: String, nombre: String, clave: String,
        rol: String = "PACIENTE", especialistaId: Long? = null): UsuarioEntity {
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return UsuarioEntity(email.trim().lowercase(), nombre.trim(), hash = codificar(derivar(clave, sal)),
            sal = codificar(sal), rol = rol, especialistaId = especialistaId)
    }
    private fun codificar(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun derivar(clave: String, sal: ByteArray): ByteArray {
        val spec = PBEKeySpec(clave.toCharArray(), sal, 120_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    suspend fun prepararDemostracion() = withContext(Dispatchers.IO) {
        if (dao.usuario(ADMIN_DEMO) == null) {
            try { dao.insertarUsuario(nuevaCuenta(ADMIN_DEMO, "Administración de demostración", CLAVE_DEMO, "ADMIN")) }
            catch (_: android.database.sqlite.SQLiteConstraintException) { /* Otra inicialización ya la creó. */ }
        }
    }
    /** Importa la única cuenta anterior y retira su contraseña en texto plano. */
    private suspend fun migrarCuentaAnterior() {
        val email = prefs.getString("email_registrado", null)?.trim()?.lowercase() ?: return
        val clave = prefs.getString("password_registrada", null) ?: return
        if (dao.usuario(email) == null) {
            try { dao.insertarUsuario(nuevaCuenta(email, prefs.getString("nombre_registrado", "Paciente").orEmpty(), clave)) }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
        }
        prefs.edit().remove("password_registrada").apply()
    }
    suspend fun registrar(nombre: String, email: String, clave: String) = withContext(Dispatchers.IO) {
        require(nombre.trim().length >= 3) { "El nombre debe tener al menos 3 caracteres" }
        require(ValidacionCuenta.correo(email) == null) { "Ingresa un correo válido" }
        require(ValidacionCuenta.clave(clave) == null) { "La contraseña debe tener al menos 6 caracteres" }
        migrarCuentaAnterior()
        require(dao.usuario(email.trim().lowercase()) == null) { "Ya existe una cuenta con ese correo" }
        dao.insertarUsuario(nuevaCuenta(email, nombre, clave))
    }
    suspend fun entrar(email: String, clave: String): UsuarioEntity? = withContext(Dispatchers.IO) {
        prepararDemostracion(); migrarCuentaAnterior()
        val u = dao.usuario(email.trim().lowercase()) ?: return@withContext null
        if (!u.activo || !MessageDigest.isEqual(derivar(clave, Base64.decode(u.sal, Base64.NO_WRAP)),
            Base64.decode(u.hash, Base64.NO_WRAP))) return@withContext null
        prefs.edit().putString("email_sesion", u.email).putString("nombre_registrado",u.nombre).apply()
        u
    }
    suspend fun editarPerfil(nombre: String, telefono: String) = withContext(Dispatchers.IO) {
        val u = actual() ?: error("Inicia sesión para editar tu perfil")
        require(nombre.trim().length >= 3) { "El nombre debe tener al menos 3 caracteres" }
        require(ValidacionCuenta.telefono(telefono) == null) { "Ingresa un teléfono válido" }
        dao.actualizarUsuario(u.copy(nombre=nombre.trim(), telefono=telefono.trim()))
        prefs.edit().putString("nombre_registrado",nombre.trim()).apply()
    }
    suspend fun cambiarClave(actual: String, nueva: String, confirmacion: String) = withContext(Dispatchers.IO) {
        val u = this@CuentaRepository.actual() ?: error("Inicia sesión")
        require(MessageDigest.isEqual(derivar(actual, Base64.decode(u.sal,0)), Base64.decode(u.hash,0))) { "La contraseña actual no coincide" }
        require(ValidacionCuenta.clave(nueva) == null) { "La nueva contraseña debe tener al menos 6 caracteres" }
        require(nueva == confirmacion) { "Las contraseñas no coinciden" }
        val renovada = nuevaCuenta(u.email,u.nombre,nueva,u.rol,u.especialistaId)
        dao.actualizarUsuario(u.copy(hash=renovada.hash, sal=renovada.sal))
    }
    suspend fun guardarFoto(uri: android.net.Uri) = withContext(Dispatchers.IO) {
        val u = actual() ?: return@withContext
        val carpeta = java.io.File(appContext.filesDir,"perfiles").apply { mkdirs() }
        val archivo = java.io.File(carpeta,"${java.util.UUID.randomUUID()}.jpg")
        appContext.contentResolver.openInputStream(uri).use { entrada ->
            requireNotNull(entrada) { "No se pudo leer la imagen" }
            archivo.outputStream().use { entrada.copyTo(it) }
        }
        dao.actualizarUsuario(u.copy(fotoUri=android.net.Uri.fromFile(archivo).toString()))
    }
    suspend fun crearEspecialista(nombre: String, email: String, clave: String, especialidad: String,
        descripcion: String, servicios: List<Long>) = withContext(Dispatchers.IO) {
        require(actual()?.rol == "ADMIN") { "Solo administración puede crear profesionales" }
        require(nombre.trim().length >= 3 && especialidad.isNotBlank() && servicios.isNotEmpty()) { "Completa nombre, especialidad y al menos un servicio" }
        require(ValidacionCuenta.correo(email) == null && ValidacionCuenta.clave(clave) == null) { "Revisa correo y contraseña" }
        require(dao.usuario(email.trim().lowercase()) == null) { "Ese correo ya está registrado" }
        db.withTransaction {
            val id = db.especialistaDao().insertar(EspecialistaEntity(nombre=nombre.trim(), especialidad=especialidad.trim(), descripcion=descripcion.trim()))
            dao.vincularServicios(servicios.distinct().map { EspecialistaServicioEntity(id,it) })
            dao.insertarUsuario(nuevaCuenta(email,nombre,clave,"ESPECIALISTA",id))
        }
    }

    /** Mantiene el perfil profesional separado de la cuenta y evita que dos cuentas compartan una agenda. */
    suspend fun cambiarRol(email: String, rol: String, especialistaId: Long?) = withContext(Dispatchers.IO) {
        db.withTransaction {
            require(actual()?.rol == "ADMIN") { "Solo administración puede cambiar roles" }
            require(rol == "PACIENTE" || rol == "ESPECIALISTA") { "Selecciona paciente o especialista" }
            val cuenta = dao.usuario(email.trim().lowercase()) ?: error("La cuenta no existe")
            require(cuenta.rol != "ADMIN") { "La cuenta administradora conserva su rol" }
            val profesional = if (rol == "ESPECIALISTA") {
                val id = requireNotNull(especialistaId) { "Selecciona el perfil profesional" }
                require(db.especialistaDao().obtenerPorId(id) != null) { "Selecciona un perfil profesional" }
                require(dao.cuentaDeEspecialista(id, cuenta.email) == null) { "Ese profesional ya tiene otra cuenta asignada" }
                id
            } else null
            dao.actualizarUsuario(cuenta.copy(rol = rol, especialistaId = profesional))
        }
    }
    companion object {
        const val ADMIN_DEMO = "admin.demo@sanaesencia.local"
        const val CLAVE_DEMO = "DemoCentro2026!"
    }
}
