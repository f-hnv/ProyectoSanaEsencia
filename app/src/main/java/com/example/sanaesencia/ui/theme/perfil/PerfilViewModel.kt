package com.example.sanaesencia.ui.perfil

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class PerfilViewModel : ViewModel() {
    private val _fotoPerfilUri = mutableStateOf<Uri?>(null)
    val fotoPerfilUri: State<Uri?> = _fotoPerfilUri

    fun actualizarFotoPerfil(uri: Uri?) {
        if (uri != null) {
            _fotoPerfilUri.value = uri
        }
    }
}