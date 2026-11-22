package com.example.sanaesencia.data.repository

import androidx.room.withTransaction
import com.example.sanaesencia.BuildConfig
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.utils.precioExacto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode

data class CatalogoEstado(
    val servicios:List<ServicioEntity> = emptyList(), val especialistas:List<EspecialistaEntity> = emptyList(),
    val serviciosPorEspecialista:Map<Long,List<Long>> = emptyMap(), val cargando:Boolean=true,
    val vigente:Boolean=false, val actualizadoEn:Long?=null, val error:String?=null
)

/** Obtiene una instantánea completa antes de actualizar la copia local, sin borrar citas ni sus referencias. */
class CatalogoRepository(private val db:AppDatabase, private val api:CatalogoApi,
    private val origen:String=BuildConfig.CATALOGO_API_URL,
    private val reloj:()->Long={System.currentTimeMillis()}) {
    private val mutex=Mutex()
    private val _estado=MutableStateFlow(CatalogoEstado())
    val estado=_estado.asStateFlow()
    private val dao=db.catalogoRemotoDao()

    suspend fun actualizar(forzar:Boolean=false) = mutex.withLock {
        val anterior=_estado.value
        if(!forzar && anterior.vigente && reloj()-(anterior.actualizadoEn ?: 0)<30_000) return@withLock
        val cache=try { leerCache() }
        catch(e:CancellationException) { throw e }
        catch(e:Exception) {
            _estado.value=anterior.copy(cargando=false,vigente=false,error="No se pudo leer la copia del catálogo. Inténtalo nuevamente")
            return@withLock
        }
        _estado.value=cache.copy(cargando=true)
        try {
            val datos=llamadaBackend { coroutineScope {
                val servicios=async { api.servicios() }
                val especialistas=async { api.especialistas() }
                servicios.await() to especialistas.await()
            } }
            validar(datos.first,datos.second)
            guardar(datos.first.filter { it.activo },datos.second.filter { it.activo })
            _estado.value=leerCache().copy(cargando=false,vigente=true)
        } catch(e:CancellationException) {
            _estado.value=cache.copy(cargando=false)
            throw e
        } catch(e:Exception) {
            _estado.value=cache.copy(cargando=false,error=(e as? ErrorBackend)?.message
                ?: "No se pudo actualizar el catálogo. Inténtalo nuevamente")
        }
    }
    private fun validar(servicios:List<ServicioRemoto>, perfiles:List<EspecialistaRemoto>) {
        fun servicioValido(s:ServicioRemoto)=s.id>0 && !s.nombre.isNullOrBlank() && s.descripcion!=null &&
            s.precio!=null && s.precio>BigDecimal.ZERO && s.precio<=BigDecimal("99999999.99") &&
            s.precio.stripTrailingZeros().scale()<=2 && s.duracionMinutos>0
        require(servicios.all(::servicioValido) && servicios.distinctBy { it.id }.size==servicios.size)
        require(perfiles.distinctBy { it.id }.size==perfiles.size && perfiles.all {
            it.id>0 && !it.nombre.isNullOrBlank() && !it.especialidad.isNullOrBlank() && it.descripcion!=null &&
                it.servicios!=null && it.servicios.all(::servicioValido) &&
                it.servicios.distinctBy { s -> s.id }.size==it.servicios.size &&
                (!it.activo || it.permiteOnline || it.permitePresencial)
        })
    }
    private suspend fun guardar(servicios:List<ServicioRemoto>, perfiles:List<EspecialistaRemoto>) = db.withTransaction {
        val vinculos=dao.vinculos(origen)
        val serviciosPrevios=vinculos.filter { it.tipo=="SERVICIO" }.associate { it.servidorId to it.localId }
        val perfilesPrevios=vinculos.filter { it.tipo=="ESPECIALISTA" }.associate { it.servidorId to it.localId }
        for(id in serviciosPrevios.values) db.servicioDao().obtenerPorId(id)?.let { db.servicioDao().actualizar(it.copy(activo=false)) }
        for(id in perfilesPrevios.values) db.especialistaDao().obtenerPorId(id)?.let { db.especialistaDao().actualizar(it.copy(activo=false)) }
        val serviciosLocales=mutableMapOf<Long,Long>()
        for(s in servicios) {
            val id=serviciosPrevios[s.id]
            val fila=ServicioEntity(id=id ?: 0,nombre=s.nombre,descripcion=s.descripcion,categoria="Servicios del centro",
                duracionMin=s.duracionMinutos,precioClp=s.precio.setScale(0,RoundingMode.HALF_UP).intValueExact(),
                permiteOnline=s.permiteOnline,activo=true,precioServidor=s.precio.toPlainString())
            val local=if(id==null) db.servicioDao().insertar(fila) else { db.servicioDao().actualizar(fila); id }
            if(id==null) dao.vincular(CatalogoVinculoEntity(origen,"SERVICIO",s.id,local))
            serviciosLocales[s.id]=local
        }
        dao.limpiarServiciosDeEspecialistas(perfilesPrevios.values.toList())
        for(p in perfiles) {
            val id=perfilesPrevios[p.id]
            val fila=EspecialistaEntity(id=id ?: 0,nombre=p.nombre,especialidad=p.especialidad,descripcion=p.descripcion,
                permiteOnline=p.permiteOnline,permitePresencial=p.permitePresencial,activo=true)
            val local=if(id==null) db.especialistaDao().insertar(fila) else { db.especialistaDao().actualizar(fila); id }
            if(id==null) dao.vincular(CatalogoVinculoEntity(origen,"ESPECIALISTA",p.id,local))
            val asignados=p.servicios.filter { it.activo }.mapNotNull { serviciosLocales[it.id] }
            db.centroDao().vincularServicios(asignados.map { EspecialistaServicioEntity(local,it) })
        }
        dao.sincronizar(CatalogoSincronizacionEntity(origen,reloj()))
    }
    private suspend fun leerCache():CatalogoEstado = db.withTransaction {
        val vinculos=dao.vinculos(origen)
        val servicios=vinculos.filter { it.tipo=="SERVICIO" }.mapNotNull { db.servicioDao().obtenerPorId(it.localId) }.filter { it.activo }
        val perfiles=vinculos.filter { it.tipo=="ESPECIALISTA" }.mapNotNull { db.especialistaDao().obtenerPorId(it.localId) }.filter { it.activo }
        CatalogoEstado(servicios.sortedBy { it.nombre },perfiles.sortedBy { it.nombre },
            perfiles.associate { it.id to db.centroDao().serviciosDeEspecialista(it.id) },
            cargando=false,actualizadoEn=dao.sincronizacion(origen)?.actualizadoEn)
    }
    suspend fun exigirVigente() {
        actualizar()
        if(!_estado.value.vigente) throw ErrorBackend(null,_estado.value.error ?: "Actualiza el catálogo para continuar")
    }
    suspend fun idServicioServidor(local:Long):Long?=dao.vinculos(origen).find { it.tipo=="SERVICIO" && it.localId==local }?.servidorId
    suspend fun idEspecialistaServidor(local:Long):Long?=dao.vinculos(origen).find { it.tipo=="ESPECIALISTA" && it.localId==local }?.servidorId
    suspend fun idEspecialistaLocal(servidor:Long):Long? {
        exigirVigente()
        return dao.vinculos(origen).find { it.tipo=="ESPECIALISTA" && it.servidorId==servidor }?.localId
    }
    suspend fun validarSeleccion(servicioLocal:Long, perfilLocal:Long, modalidad:Modalidad) {
        val sId=idServicioServidor(servicioLocal) ?: throw ErrorBackend(409,"Vuelve a elegir el servicio desde el catálogo actualizado")
        val pId=idEspecialistaServidor(perfilLocal) ?: throw ErrorBackend(409,"Vuelve a elegir el especialista desde el catálogo actualizado")
        val (s,p)=llamadaBackend { coroutineScope {
            val servicio=async { api.servicio(sId) }; val perfil=async { api.especialista(pId) }
            servicio.await() to perfil.await()
        } }
        validar(listOf(s),listOf(p))
        val anterior=db.servicioDao().obtenerPorId(servicioLocal)
        val profesional=db.especialistaDao().obtenerPorId(perfilLocal)
        val valido=anterior!=null && profesional!=null && s.id==sId && p.id==pId && s.activo && p.activo && p.servicios.any { it.id==sId && it.activo } &&
            s.duracionMinutos==anterior?.duracionMin && s.precio.compareTo(anterior.precioExacto())==0 &&
            s.nombre==anterior.nombre && s.permiteOnline==anterior.permiteOnline &&
            p.permiteOnline==profesional?.permiteOnline && p.permitePresencial==profesional.permitePresencial &&
            (if(modalidad==Modalidad.ONLINE) s.permiteOnline && p.permiteOnline else p.permitePresencial)
        if(!valido) {
            actualizar(forzar=true)
            throw ErrorBackend(409,"El servicio o especialista cambió. Vuelve a elegir desde el catálogo actualizado")
        }
    }
}
