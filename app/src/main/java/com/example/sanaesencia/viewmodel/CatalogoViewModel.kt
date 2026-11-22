package com.example.sanaesencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanaesencia.data.repository.CatalogoRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class CatalogoViewModel(private val catalogo:CatalogoRepository):ViewModel() {
    val uiState=catalogo.estado
    private var carga:Job?=null
    init { actualizar() }
    fun actualizar() {
        if(carga?.isActive==true) return
        carga=viewModelScope.launch { catalogo.actualizar(forzar=true) }
    }
}
