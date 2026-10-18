package com.example.sanaesencia.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.viewmodel.LoginViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.ui.components.SanaPrimaryButton as Button

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegistro: () -> Unit,
    onNavigateToContacto: () -> Unit = {},
    viewModel: LoginViewModel = viewModel()
) {
    val email = viewModel.email.value
    val emailError = viewModel.emailError.value
    val password = viewModel.password.value
    val passwordError = viewModel.passwordError.value
    val cargando = viewModel.cargando.value // Leemos el estado de carga
    val accesoConcedido = viewModel.accesoConcedido.value
    val teclado = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .animateContentSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sanaesencia",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        AnimatedVisibility(visible = cargando || accesoConcedido,
            enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut()) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                AnimatedContent(targetState = accesoConcedido, label = "Resultado del inicio de sesión") { exito ->
                    Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        if (exito) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("¡Bienvenido a Sanaesencia!", style = MaterialTheme.typography.titleMedium)
                                Text("Tu sesión está lista", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            CircularProgressIndicator(Modifier.size(24.dp))
                            Text("Validando tu acceso…")
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text("Correo Electrónico") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Ícono de correo") },
            trailingIcon = {
                if (emailError != null) Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
            },
            isError = emailError != null,
            supportingText = { if (emailError != null) Text(emailError) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargando // Bloqueamos el campo si está cargando
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Ícono de candado") },
            trailingIcon = {
                if (passwordError != null) Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
            },
            isError = passwordError != null,
            supportingText = { if (passwordError != null) Text(passwordError) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargando // Bloqueamos el campo si está cargando
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                teclado?.hide()
                viewModel.login(context = context, onSuccess = onNavigateToHome)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !cargando // Bloqueamos el botón para evitar doble clic
        ) {
            if (cargando) {
                // Mostramos el spinner
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Ingresar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegistro, enabled = !cargando) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
        TextButton(onClick = onNavigateToContacto, enabled = !cargando) { Text("Contáctanos") }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    SanaesenciaTheme {
        LoginScreen(
            onNavigateToHome = {},
            onNavigateToRegistro = {}
        )
    }
}

