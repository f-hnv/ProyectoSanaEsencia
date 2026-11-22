package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.remote.*
import com.example.sanaesencia.data.repository.ReservasRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class GestionBoxesState(val boxes:List<BoxRemoto> = emptyList(),val perfiles:List<EspecialistaRemoto> = emptyList(),
    val ocupada:Boolean=false,val mensaje:String?=null)
class AdministracionReservasViewModel(private val repo:ReservasRepository):ViewModel() {
    private val _estado=MutableStateFlow(GestionBoxesState());val estado=_estado.asStateFlow()
    init { cargar() }
    fun cargar()=operar {
        val boxes=repo.boxes();val perfiles=llamadaBackend { ConexionBackend.catalogo.especialistas() }.filter {it.activo}
        _estado.update {it.copy(boxes=boxes,perfiles=perfiles)}
    }
    fun crear(nombre:String,descripcion:String)=operar {
        require(nombre.trim().length in 2..100) { "El nombre del box debe tener entre 2 y 100 caracteres" }
        repo.crearBox(nombre,descripcion);_estado.update {it.copy(boxes=repo.boxes(),mensaje="Box creado")}
    }
    fun activar(id:Long,activo:Boolean)=operar {repo.estadoBox(id,activo);_estado.update {it.copy(boxes=repo.boxes(),mensaje="Estado del box actualizado")}}
    private fun operar(bloque:suspend ()->Unit) {
        if(_estado.value.ocupada)return
        _estado.update {it.copy(ocupada=true,mensaje=null)}
        viewModelScope.launch {
            try {bloque()}
            catch(e:CancellationException){throw e}
            catch(e:Exception){_estado.update {it.copy(mensaje=e.message ?: "No se pudo completar la operación")}}
            finally {_estado.update {it.copy(ocupada=false)}}
        }
    }
}
data class HorariosServidorState(val horarios:List<HorarioRemoto> = emptyList(),val ocupada:Boolean=false,val mensaje:String?=null,val cargado:Boolean=false)
class HorariosServidorViewModel(private val repo:ReservasRepository):ViewModel() {
    private val _estado=MutableStateFlow(HorariosServidorState());val estado=_estado.asStateFlow()
    private var perfil:Long?=null
    fun cargar(id:Long) {
        if(_estado.value.ocupada)return
        perfil=id
        operar {val lista=repo.horarios(id);_estado.update {it.copy(horarios=lista.map {h->h.copy(desde=h.desde.take(5),hasta=h.hasta.take(5))},cargado=true)}}
    }
    fun editar(indice:Int,h:HorarioRemoto) {if(!_estado.value.ocupada)_estado.update {it.copy(horarios=it.horarios.toMutableList().apply {this[indice]=h},mensaje=null)}}
    fun agregar() {if(!_estado.value.ocupada && _estado.value.horarios.size<28)_estado.update {it.copy(horarios=it.horarios+HorarioRemoto("MONDAY","09:00","19:00"),mensaje=null)}}
    fun quitar(i:Int) {if(!_estado.value.ocupada)_estado.update {it.copy(horarios=it.horarios.filterIndexed {j,_->i!=j},mensaje=null)}}
    fun guardar() {
        val id=perfil ?: return
        val lista=_estado.value.horarios
        operar {
            require(lista.isNotEmpty()) {"Agrega al menos una franja horaria"}
            val patron=Regex("([01][0-9]|2[0-3]):00")
            val horaPublicada=Regex("([01][0-9]|2[0-3]):[0-5][0-9]")
            require(lista.all {h->if(h.cupoPublicado) h.fecha!=null && horaPublicada.matches(h.desde) && horaPublicada.matches(h.hasta) && h.desde<h.hasta
                else patron.matches(h.desde) && patron.matches(h.hasta) && h.desde<h.hasta && h.desde>="09:00" && h.hasta<="19:00" && h.dia !in listOf("SATURDAY","SUNDAY")}) {"Revisa las horas. Los cupos publicados requieren fecha; las franjas semanales usan horas completas de lunes a viernes"}
            val guardado=repo.guardarHorarios(id,lista)
            _estado.update {it.copy(horarios=guardado.map {h->h.copy(desde=h.desde.take(5),hasta=h.hasta.take(5))},mensaje="Horarios guardados en el servidor")}
        }
    }
    private fun operar(bloque:suspend ()->Unit) {
        if(_estado.value.ocupada)return
        _estado.update {it.copy(ocupada=true,mensaje=null)}
        viewModelScope.launch {
            try {bloque()}
            catch(e:CancellationException){throw e}
            catch(e:Exception){_estado.update {it.copy(mensaje=e.message ?: "No se pudo guardar la agenda")}}
            finally {_estado.update {it.copy(ocupada=false)}}
        }
    }
}
