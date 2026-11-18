package com.example.sanaesencia.ui.components
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.sanaesencia.R

@Composable
fun LogoSanaesencia(modifier:Modifier=Modifier) {
    Image(painterResource(R.drawable.sanaesencia_logo),contentDescription="Centro Integrativo Sanaesencia",modifier=modifier.height(50.dp))
}
/** Pulso de transición inspirado en el símbolo de carga del sitio, sin rotar el logotipo. */
@Composable
fun CargaSanaesencia(texto:String="Sanaesencia") {
    val transicion=rememberInfiniteTransition(label="Pulso del símbolo")
    val escala by transicion.animateFloat(.92f,1.04f,infiniteRepeatable(tween(650),RepeatMode.Reverse),label="Escala")
    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Image(painterResource(R.drawable.sanaesencia_simbolo),contentDescription="",modifier=Modifier.size(96.dp).graphicsLayer{scaleX=escala;scaleY=escala})
        Text(texto,style=MaterialTheme.typography.labelLarge)
    }
}
