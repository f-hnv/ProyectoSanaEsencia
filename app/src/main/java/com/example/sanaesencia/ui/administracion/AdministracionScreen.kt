package com.example.sanaesencia.ui.administracion

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.sanaesencia.data.remote.SesionBackend

@Composable
fun AdministracionScreen() {
    if (SesionBackend.actual()?.usuario?.rol == "ADMIN") AdministracionServidor()
    else Text("Inicia sesión con una cuenta administradora")
}
