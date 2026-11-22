package com.example.sanaesencia

import androidx.test.core.app.ApplicationProvider
import com.example.sanaesencia.data.remote.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.Credentials
import okhttp3.mockwebserver.*
import org.junit.rules.ExternalResource
import java.util.concurrent.ConcurrentHashMap

/** Simula HTTP, nunca crea administradores o roles en producción. */
class BackendUsuariosFixture: ExternalResource() {
    private val gson=Gson()
    private lateinit var app:SanaesenciaApp
    private lateinit var anterior:UsuariosApi
    private lateinit var perfilesAnteriores:PerfilesApi
    private lateinit var servidor:MockWebServer
    private lateinit var catalogoAnterior:com.example.sanaesencia.data.repository.CatalogoRepository
    private lateinit var apiCatalogoAnterior:CatalogoApi
    private lateinit var reservasAnterior:com.example.sanaesencia.data.repository.ReservasRepository
    val clave="ClaveSoloFixture2026!"
    val cuentas=ConcurrentHashMap<Long,UsuarioRemoto>()
    val claves=ConcurrentHashMap<String,String>()
    override fun before() {
        app=ApplicationProvider.getApplicationContext()
        anterior=app.container.usuariosApi; perfilesAnteriores=app.container.perfilesApi
        catalogoAnterior=app.container.catalogoRepository
        apiCatalogoAnterior=app.container.catalogoApi;reservasAnterior=app.container.reservasRepository
        val servicio=ServicioRemoto(101,"Servicio de prueba","Descripción de prueba",java.math.BigDecimal("30000.00"),60,true,true)
        val profesional=EspecialistaRemoto(1,"Especialista de prueba","Terapia","Perfil ficticio",true,true,true,listOf(servicio))
        val catalogo=object:CatalogoApi {
            override suspend fun servicios()=listOf(servicio)
            override suspend fun especialistas(servicioId:Long?)=if(servicioId==null || servicioId==101L) listOf(profesional) else emptyList()
            override suspend fun servicio(id:Long)=servicio
            override suspend fun especialista(id:Long)=profesional
        }
        app.container.catalogoRepository=com.example.sanaesencia.data.repository.CatalogoRepository(app.container.database,catalogo,"fixture-${java.util.UUID.randomUUID()}")
        app.container.catalogoApi=catalogo
        SesionBackend.cerrar()
        cuentas.clear(); claves.clear()
        for ((id,rol) in listOf(42L to "PACIENTE",43L to "ESPECIALISTA",4L to "ADMIN")) {
            val u=UsuarioRemoto(id,"Cuenta","Prueba","12345678-5","${rol.lowercase()}.fixture@example.com",rol,if(rol=="ESPECIALISTA") 1 else null,true)
            cuentas[id]=u; claves[u.correo]=clave
        }
        servidor=MockWebServer()
        servidor.dispatcher=object:Dispatcher() {
            override fun dispatch(request:RecordedRequest):MockResponse {
                val ruta=request.path.orEmpty()
                fun respuesta(codigo:Int, cuerpo:Any)=MockResponse().setResponseCode(codigo).setHeader("Content-Type","application/json").setBody(gson.toJson(cuerpo))
                if (ruta=="/api/usuarios/login") {
                    val body=JsonParser.parseString(request.body.readUtf8()).asJsonObject
                    val u=cuentas.values.find { it.correo==body["correo"].asString && it.activo }
                    return if(u!=null && claves[u.correo]==body["password"].asString) respuesta(200,u)
                    else respuesta(401,mapOf("mensaje" to "Credenciales incorrectas"))
                }
                val auth=cuentas.values.find { it.activo && request.getHeader("Authorization")==Credentials.basic(it.correo,claves[it.correo].orEmpty(),Charsets.UTF_8) }
                    ?: return respuesta(401,mapOf("mensaje" to "Inicia sesión"))
                if(request.method=="GET" && (ruta.startsWith("/api/reservas?") || ruta.startsWith("/api/agenda/"))) return respuesta(200,emptyList<Any>())
                val id=ruta.split('/').getOrNull(if(ruta.startsWith("/api/admin/")) 4 else 3)?.toLongOrNull()
                    ?: return respuesta(404,mapOf("mensaje" to "No encontrado"))
                val u=cuentas[id] ?: return respuesta(404,mapOf("mensaje" to "No encontrado"))
                if (request.method=="PATCH" && ruta=="/api/admin/usuarios/$id/rol") {
                    if(auth.rol!="ADMIN") return respuesta(403,mapOf("mensaje" to "Sin permiso"))
                    val body=JsonParser.parseString(request.body.readUtf8()).asJsonObject
                    val actualizado=u.copy(rol=body["rol"].asString,especialistaId=body["especialistaId"]?.takeUnless { it.isJsonNull }?.asLong)
                    cuentas[id]=actualizado; return respuesta(200,actualizado)
                }
                if (auth.id!=id && auth.rol!="ADMIN") return respuesta(403,mapOf("mensaje" to "Sin permiso"))
                if (request.method=="PUT") {
                    val body=JsonParser.parseString(request.body.readUtf8()).asJsonObject
                    val actualizado=u.copy(nombre=body["nombre"].asString,apellido=body["apellido"].asString)
                    cuentas[id]=actualizado; return respuesta(200,actualizado)
                }
                return respuesta(200,u)
            }
        }
        servidor.start()
        app.container.usuariosApi=ConexionBackend.crearUsuarios(servidor.url("/").toString())
        app.container.reservasRepository=com.example.sanaesencia.data.repository.ReservasRepository(ConexionBackend.crearReservas(servidor.url("/").toString()))
        app.container.perfilesApi=object:PerfilesApi {
            override suspend fun obtener(id:Long,autorizacion:String)=PerfilProfesionalRemoto(id,"Perfil de prueba",true)
        }
    }
    override fun after() {
        SesionBackend.cerrar(); app.container.usuariosApi=anterior; app.container.perfilesApi=perfilesAnteriores
        app.container.catalogoRepository=catalogoAnterior
        app.container.catalogoApi=apiCatalogoAnterior;app.container.reservasRepository=reservasAnterior
        servidor.shutdown()
    }
}
