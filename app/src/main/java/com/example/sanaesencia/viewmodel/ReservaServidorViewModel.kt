package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.repository.*
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.di.AppContainer
import com.example.sanaesencia.utils.FechasReservas
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ReservaServidorState(
    val cargando:Boolean=true, val ocupada:Boolean=false, val error:String?=null,
    val servicio:ServicioRemoto?=null, val profesionales:List<EspecialistaRemoto> = emptyList(),
    val especialistaId:Long?=null, val modalidad:Modalidad=Modalidad.PRESENCIAL,
    val dia:String?=null, val inicio:String?=null, val turnos:List<TurnoRemoto> = emptyList(),
    val cargandoTurnos:Boolean=false, val errorTurnos:String?=null,
    val recibo:ReservaRemota?=null, val ambigua:Boolean=false, val sugerencia:SugerenciaRemota?=null,
    val errorUbicacion:String?=null, val errorCarga:Boolean=false
)

class ReservaServidorViewModel(private val container:AppContainer):ViewModel() {
    private val repo=container.reservasRepository
    private val _estado=MutableStateFlow(ReservaServidorState())
    val estado=_estado.asStateFlow()
    private var edicion:Long?=null
    private var carga:Job?=null
    private var consulta:Job?=null
    private var gps:Job?=null
    private var coordenadas:Pair<Double,Double>?=null
    private var entrada:Triple<Long?,Long?,Long?>?=null
    val idsLocales=mutableMapOf<Long,Long>()
    fun preparar(servicioLocal:Long?,perfilLocal:Long?,reservaId:Long?) {
        val clave=Triple(servicioLocal,perfilLocal,reservaId)
        if(entrada==clave && !_estado.value.errorCarga) return
        entrada=clave; edicion=reservaId; carga?.cancel(); consulta?.cancel();gps?.cancel()
        _estado.value=ReservaServidorState()
        carga=viewModelScope.launch {
            try {
                val previa=reservaId?.let { repo.obtener(it) }
                if(previa!=null && !FechasReservas.futura(previa)) throw ErrorBackend(409,"La cita ya no puede reprogramarse")
                val catalogo=container.catalogoRepository
                catalogo.exigirVigente()
                val id=previa?.servicioId ?: servicioLocal?.let { catalogo.idServicioServidor(it) }
                    ?: throw ErrorBackend(409,"Elige un servicio desde el catálogo actualizado")
                val servicio=llamadaBackend { container.catalogoApi.servicio(id) }
                val perfiles=llamadaBackend { container.catalogoApi.especialistas(id) }.filter { it.activo && it.servicios.any { s->s.id==id && s.activo } }
                require(servicio.activo) { "El servicio no está disponible" }
                idsLocales.clear()
                for(p in perfiles) catalogo.idEspecialistaLocal(p.id)?.let { idsLocales[p.id]=it }
                val elegido=previa?.especialistaId ?: perfilLocal?.let { catalogo.idEspecialistaServidor(it) }
                val profesional=perfiles.find { it.id==elegido }
                if(previa!=null && profesional==null) throw ErrorBackend(409,"El especialista ya no ofrece este servicio")
                val modalidad=previa?.modalidad ?: if(profesional?.permitePresencial==false) Modalidad.ONLINE else Modalidad.PRESENCIAL
                _estado.value=ReservaServidorState(cargando=false,servicio=servicio,profesionales=perfiles,
                    especialistaId=profesional?.id,modalidad=modalidad,dia=previa?.inicio?.take(10))
                cargarTurnos()
            } catch(e:CancellationException) { throw e }
            catch(e:Exception) { _estado.value=ReservaServidorState(cargando=false,error=e.message ?: "No se pudo preparar la reserva",errorCarga=true) }
        }
    }
    fun reintentar() { entrada?.let { preparar(it.first,it.second,it.third) } }
    fun especialista(id:Long) {
        if(edicion!=null || _estado.value.ocupada) return
        val p=_estado.value.profesionales.find { it.id==id } ?: return
        _estado.update { it.copy(especialistaId=id,modalidad=if(p.permitePresencial) Modalidad.PRESENCIAL else Modalidad.ONLINE,inicio=null,error=null) }
        cargarTurnos(); ubicacion(coordenadas)
    }
    fun modalidad(m:Modalidad) {
        if(_estado.value.ocupada) return
        _estado.update { it.copy(modalidad=m,inicio=null,error=null) }; cargarTurnos()
    }
    fun dia(d:String) { if(!_estado.value.ocupada) { _estado.update { it.copy(dia=d,inicio=null,error=null) }; cargarTurnos() } }
    fun hora(inicio:String) { if(!_estado.value.ocupada && _estado.value.turnos.any { it.inicio==inicio }) _estado.update { it.copy(inicio=inicio,error=null) } }
    fun cargarTurnos() {
        consulta?.cancel()
        val s=_estado.value
        _estado.update { it.copy(turnos=emptyList(),inicio=null,errorTurnos=null,cargandoTurnos=false) }
        val p=s.especialistaId ?: return; val servicio=s.servicio ?: return; val dia=s.dia ?: return
        _estado.update { it.copy(cargandoTurnos=true) }
        consulta=viewModelScope.launch {
            try { val turnos=repo.disponibilidad(p,servicio.id,dia,s.modalidad,edicion)
                _estado.update { it.copy(turnos=turnos,cargandoTurnos=false) }
            } catch(e:CancellationException) { throw e }
            catch(e:Exception) { _estado.update { it.copy(cargandoTurnos=false,errorTurnos=e.message ?: "No se pudo consultar disponibilidad") } }
        }
    }
    fun ubicacion(valor:Pair<Double,Double>?) {
        gps?.cancel(); coordenadas=valor
        _estado.update { it.copy(sugerencia=null,errorUbicacion=null) }
        val s=_estado.value;val p=s.especialistaId ?: return;val servicio=s.servicio ?: return
        if(valor==null) return
        gps=viewModelScope.launch {
            try { val sugerencia=repo.sugerencia(p,servicio.id,valor.first,valor.second); _estado.update { it.copy(sugerencia=sugerencia) } }
            catch(e:CancellationException) { throw e }
            catch(e:Exception) { _estado.update { it.copy(errorUbicacion="No se pudo consultar la recomendación. Puedes elegir la modalidad disponible.") } }
        }
    }
    fun guardar() {
        val s=_estado.value
        if(s.ocupada || s.ambigua || s.recibo!=null || s.inicio==null || s.turnos.none { it.inicio==s.inicio }) return
        val id=s.servicio?.id ?: return;val perfil=s.especialistaId ?: return
        _estado.update { it.copy(ocupada=true,error=null) }
        viewModelScope.launch {
            try {
                val servicioActual=llamadaBackend { container.catalogoApi.servicio(id) }
                val perfilActual=llamadaBackend { container.catalogoApi.especialista(perfil) }
                if(!servicioActual.activo || !perfilActual.activo || perfilActual.servicios.none {it.id==id && it.activo})
                    throw ErrorBackend(409,"La atención ya no está disponible. Vuelve a elegir desde el catálogo")
                if(servicioActual.precio.compareTo(s.servicio!!.precio)!=0 || servicioActual.duracionMinutos!=s.servicio.duracionMinutos ||
                    servicioActual.permiteOnline!=s.servicio.permiteOnline ||
                    (s.modalidad==Modalidad.ONLINE && !perfilActual.permiteOnline) || (s.modalidad==Modalidad.PRESENCIAL && !perfilActual.permitePresencial)) {
                    _estado.update {it.copy(servicio=servicioActual)}
                    throw ErrorBackend(409,"La atención cambió. Revisa el precio y la modalidad antes de elegir otra hora")
                }
                val r=repo.guardar(edicion,ReservaSolicitud(id,perfil,s.inicio,s.modalidad,coordenadas?.first,coordenadas?.second))
                _estado.update { it.copy(ocupada=false,recibo=r) }
            } catch(e:CancellationException) { throw e }
            catch(e:Exception) {
                val incierta=e !is ErrorBackend || e.estado==null || (e.estado ?: 0)>=500
                _estado.update { it.copy(ocupada=false,ambigua=incierta,error=if(incierta)
                    "No pudimos comprobar el resultado. Revisa tus citas antes de volver a reservar para evitar duplicados."
                    else e.message ?: "No se pudo guardar la reserva") }
                if(!incierta) cargarTurnos()
            }
        }
    }
}
