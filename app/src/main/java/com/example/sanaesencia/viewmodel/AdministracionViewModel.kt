package com.example.sanaesencia.viewmodel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.local.relation.CitaConDetalle
import com.example.sanaesencia.data.repository.CuentaRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction

class AdministracionViewModel(app:Application):AndroidViewModel(app) {
    private val cuenta=CuentaRepository(app)
    var usuario by mutableStateOf<UsuarioEntity?>(null); private set
    var usuarios by mutableStateOf<List<UsuarioEntity>>(emptyList()); private set
    var boxes by mutableStateOf<List<BoxEntity>>(emptyList()); private set
    var citas by mutableStateOf<List<CitaConDetalle>>(emptyList()); private set
    var servicios by mutableStateOf<List<ServicioEntity>>(emptyList()); private set
    var mensaje by mutableStateOf<String?>(null); private set
    var ocupado by mutableStateOf(false); private set
    init { viewModelScope.launch {
        usuario=cuenta.actual()
        if(usuario==null || usuario?.rol=="PACIENTE") return@launch
        launch { cuenta.db.citaDao().observarTodas().collect { todas -> citas=todas.filter { usuario?.rol=="ADMIN" || it.cita.especialistaId==usuario?.especialistaId } } }
        if(usuario?.rol=="ADMIN") {
            launch { cuenta.db.centroDao().usuarios().collect { usuarios=it } }
            launch { cuenta.db.centroDao().boxes().collect { boxes=it } }
            launch { cuenta.db.servicioDao().observarActivos().collect { servicios=it } }
        }
    } }
    fun crear(nombre:String,email:String,clave:String,especialidad:String,descripcion:String,servicios:List<Long>) = operar {
        cuenta.crearEspecialista(nombre,email,clave,especialidad,descripcion,servicios)
        "Profesional y cuenta creados. Ya puede iniciar sesión con sus credenciales."
    }
    fun guardarUmbral(valor:String)=operar {
        require(cuenta.actual()?.rol=="ADMIN") { "Acceso no autorizado" }
        val km=valor.toFloatOrNull() ?: error("Ingresa una distancia válida")
        require(km in 1f..1000f){"El umbral debe estar entre 1 y 1000 km"}
        com.example.sanaesencia.domain.model.ConfiguracionCentro.configurar(getApplication(),km)
        "Umbral de sugerencia online actualizado"
    }
    fun alternarBox(box:BoxEntity) = operar {
        require(cuenta.actual()?.rol=="ADMIN") { "Acceso no autorizado" }
        cuenta.db.withTransaction {
            if(box.activo) require(cuenta.db.citaDao().observarTodas().first().none {
                it.cita.boxId==box.id && it.cita.estado==com.example.sanaesencia.domain.model.EstadoCita.PROGRAMADA &&
                    it.cita.fechaHoraInicio+it.cita.duracionMin*60000L>System.currentTimeMillis()
            }) { "Este box tiene citas programadas. Reprográmalas antes de desactivarlo." }
            cuenta.db.centroDao().actualizarBox(box.copy(activo=!box.activo))
        }; "Disponibilidad del box actualizada"
    }
    private fun operar(bloque:suspend ()->String) {
        if(ocupado) return
        ocupado=true
        viewModelScope.launch { try{mensaje=bloque()}catch(e:Exception){mensaje=e.message ?: "No se pudo completar"}finally{ocupado=false} }
    }
}
