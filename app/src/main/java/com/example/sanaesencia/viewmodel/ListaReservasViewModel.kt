package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.remote.ReservaRemota
import com.example.sanaesencia.data.repository.ReservasRepository
import com.example.sanaesencia.domain.model.PeriodoAgenda
import com.example.sanaesencia.utils.FechasReservas
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ListaReservasState(val periodo:PeriodoAgenda=PeriodoAgenda.desde(),val citas:List<ReservaRemota> = emptyList(),
    val cargando:Boolean=true,val error:String?=null,val mensaje:String?=null,val ocupada:Boolean=false)
class ListaReservasViewModel(private val repo:ReservasRepository):ViewModel() {
    private val _estado=MutableStateFlow(ListaReservasState());val estado=_estado.asStateFlow()
    private var consulta:Job?=null
    init { cargar();viewModelScope.launch { repo.cambios.collect { cargar() } } }
    fun mes(delta:Int) { if(!_estado.value.ocupada) { _estado.update { it.copy(periodo=it.periodo.desplazar(delta),citas=emptyList()) };cargar() } }
    fun cargar() {
        consulta?.cancel(); val mes=FechasReservas.mes(_estado.value.periodo)
        _estado.update { it.copy(cargando=true,error=null) }
        consulta=viewModelScope.launch {
            try { val citas=repo.listar(mes);_estado.update { it.copy(cargando=false,citas=citas) } }
            catch(e:CancellationException) { throw e }
            catch(e:Exception) { _estado.update { it.copy(cargando=false,citas=emptyList(),error=e.message ?: "No se pudieron cargar las citas") } }
        }
    }
    fun cambiarEstado(id:Long,atendida:Boolean=false) {
        if(_estado.value.ocupada) return
        _estado.update { it.copy(ocupada=true,mensaje=null) }
        viewModelScope.launch {
            try { if(atendida) repo.atender(id) else repo.cancelar(id)
                _estado.update { it.copy(mensaje=if(atendida) "Sesión marcada como atendida" else "Reserva cancelada") };cargar()
            } catch(e:CancellationException) { throw e }
            catch(e:Exception) { _estado.update { it.copy(mensaje=e.message ?: "Revisa el estado de la cita antes de reintentar") };cargar() }
            finally { _estado.update { it.copy(ocupada=false) } }
        }
    }
}
