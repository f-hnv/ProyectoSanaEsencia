package com.example.sanaesencia.data.repository

import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.domain.model.Modalidad
import com.example.sanaesencia.utils.FechasReservas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Reservas del servidor, sin escrituras ni respaldo de confirmaciones en Room. */
class ReservasRepository(private val api:ReservasApi,
    private val sesion:()->SesionBackend.Sesion?=SesionBackend::actual) {
    private val _cambios=MutableSharedFlow<Unit>(extraBufferCapacity=8)
    val cambios=_cambios.asSharedFlow()
    private suspend fun <T> consultar(bloque:suspend (SesionBackend.Sesion)->T):T {
        val actual=sesion() ?: throw ErrorBackend(401,"Inicia sesión para consultar tus reservas")
        val resultado=try { llamadaBackend { bloque(actual) } }
        catch(e:ErrorBackend) {
            throw ErrorBackend(e.estado,when(e.estado) {
                401 -> "Tu sesión no es válida. Vuelve a iniciar sesión"
                404 -> "La reserva, el servicio o el especialista ya no está disponible"
                else -> e.message
            })
        }
        if(sesion()?.autorizacion!=actual.autorizacion || sesion()?.usuario?.id!=actual.usuario.id)
            throw CancellationException("La sesión cambió durante la consulta")
        return resultado
    }
    private fun validar(r:ReservaRemota,u:UsuarioRemoto) {
        require(r.id>0 && r.pacienteId>0 && r.especialistaId>0 && r.servicioId>0 &&
            !r.pacienteNombre.isNullOrBlank() && !r.especialistaNombre.isNullOrBlank() && !r.servicioNombre.isNullOrBlank() &&
            r.precio!=null && r.precio.signum()>0 && r.estado in setOf("CONFIRMADA","CANCELADA","ATENDIDA") &&
            FechasReservas.instante(r.fin)>FechasReservas.instante(r.inicio) &&
            (if(r.modalidad==Modalidad.ONLINE) r.boxId==null && r.boxNombre==null else (r.boxId ?: 0)>0 && !r.boxNombre.isNullOrBlank())) {
            "El servidor devolvió una reserva no válida"
        }
        require(u.rol=="ADMIN" || (u.rol=="PACIENTE" && r.pacienteId==u.id) ||
            (u.rol=="ESPECIALISTA" && r.especialistaId==u.especialistaId)) { "La reserva no corresponde a tu cuenta" }
    }
    suspend fun listar(mes:String)=consultar { s -> api.listar(mes,s.autorizacion).also { lista ->
        lista.forEach { validar(it,s.usuario) }; require(lista.distinctBy { it.id }.size==lista.size)
    } }
    suspend fun obtener(id:Long)=consultar { s -> api.obtener(id,s.autorizacion).also { validar(it,s.usuario); require(it.id==id) } }
    suspend fun guardar(id:Long?,solicitud:ReservaSolicitud):ReservaRemota {
        require((solicitud.latitud==null)==(solicitud.longitud==null)) { "La ubicación debe incluir ambas coordenadas" }
        require(solicitud.latitud==null || (solicitud.latitud.isFinite() && solicitud.latitud in -90.0..90.0 &&
            solicitud.longitud!!.isFinite() && solicitud.longitud in -180.0..180.0)) { "La ubicación no es válida" }
        val reserva=consultar { s ->
            require(s.usuario.rol=="PACIENTE" || (id!=null && s.usuario.rol=="ADMIN")) { "Solo el paciente puede crear su reserva" }
            (if(id==null) api.crear(solicitud,s.autorizacion) else api.reprogramar(id,solicitud,s.autorizacion)).also {
                validar(it,s.usuario); require(id==null || it.id==id)
            }
        }
        _cambios.tryEmit(Unit)
        return reserva
    }
    suspend fun cancelar(id:Long) { consultar { api.cancelar(id,it.autorizacion) }; _cambios.tryEmit(Unit) }
    suspend fun atender(id:Long):ReservaRemota {
        val r=consultar { s -> require(s.usuario.rol in setOf("ESPECIALISTA","ADMIN"));
            api.estado(id,EstadoReservaSolicitud("ATENDIDA"),s.autorizacion).also { validar(it,s.usuario); require(it.id==id) } }
        _cambios.tryEmit(Unit); return r
    }
    suspend fun disponibilidad(especialista:Long,servicio:Long,fecha:String,modalidad:Modalidad,reserva:Long?)=consultar { s ->
        api.disponibilidad(especialista,servicio,fecha,modalidad,reserva,s.autorizacion).also { turnos ->
            require(turnos.map { it.inicio }.distinct().size==turnos.size)
            require(turnos.all { FechasReservas.instante(it.fin)>FechasReservas.instante(it.inicio) && it.inicio.startsWith("${fecha}T") &&
                (modalidad==Modalidad.ONLINE || it.boxesDisponibles.isNotEmpty()) }) { "La disponibilidad recibida no es válida" }
        }
    }
    suspend fun sugerencia(especialista:Long,servicio:Long,latitud:Double?,longitud:Double?)=consultar { s ->
        api.sugerencia(especialista,servicio,latitud,longitud,s.autorizacion).also {
            require(it.distanciaKm==null || (it.distanciaKm.isFinite() && it.distanciaKm>=0))
        }
    }
    suspend fun horarios(especialista:Long)=consultar { api.horarios(especialista,it.autorizacion) }
    suspend fun guardarHorarios(especialista:Long,horarios:List<HorarioRemoto>):List<HorarioRemoto> {
        require(horarios.isNotEmpty()) { "Agrega al menos un horario" }
        val resultado=consultar { s ->
            require(s.usuario.rol=="ADMIN" || (s.usuario.rol=="ESPECIALISTA" && s.usuario.especialistaId==especialista)) { "No puedes editar esta agenda" }
            api.guardarHorarios(especialista,HorariosSolicitud(horarios),s.autorizacion)
        }
        _cambios.tryEmit(Unit); return resultado
    }
    suspend fun boxes()=consultar { s -> require(s.usuario.rol=="ADMIN"); api.boxes(s.autorizacion) }
    suspend fun crearBox(nombre:String,descripcion:String)=consultar { s ->
        require(s.usuario.rol=="ADMIN"); api.crearBox(BoxSolicitud(nombre.trim(),descripcion.trim()),s.autorizacion)
    }
    suspend fun estadoBox(id:Long,activo:Boolean):BoxRemoto {
        val box=consultar { s -> require(s.usuario.rol=="ADMIN"); api.estadoBox(id,EstadoBoxSolicitud(activo),s.autorizacion) }
        _cambios.tryEmit(Unit); return box
    }
}
