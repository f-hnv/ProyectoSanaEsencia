package com.example.sanaesencia.ui.registro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.ui.components.SanaPrimaryButton as Button

@Composable
fun RegistroScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: RegistroViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    // 1. OBTENEMOS EL CONTEXTO AQUÍ
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp, top = 24.dp)
        )

        OutlinedTextField(
            value = viewModel.nombre.value,
            onValueChange = { viewModel.onNombreChange(it) },
            label = { Text("Nombre Completo") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Ícono de persona") },
            trailingIcon = { if (viewModel.nombreError.value != null) Icon(Icons.Filled.Warning, "Error", tint = MaterialTheme.colorScheme.error) },
            isError = viewModel.nombreError.value != null,
            supportingText = { if (viewModel.nombreError.value != null) Text(viewModel.nombreError.value!!) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.email.value,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text("Correo Electrónico") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Ícono de correo") },
            trailingIcon = { if (viewModel.emailError.value != null) Icon(Icons.Filled.Warning, "Error", tint = MaterialTheme.colorScheme.error) },
            isError = viewModel.emailError.value != null,
            supportingText = { if (viewModel.emailError.value != null) Text(viewModel.emailError.value!!) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.password.value,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Ícono de candado") },
            trailingIcon = { if (viewModel.passwordError.value != null) Icon(Icons.Filled.Warning, "Error", tint = MaterialTheme.colorScheme.error) },
            isError = viewModel.passwordError.value != null,
            supportingText = { if (viewModel.passwordError.value != null) Text(viewModel.passwordError.value!!) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.confirmPassword.value,
            onValueChange = { viewModel.onConfirmPasswordChange(it) },
            label = { Text("Confirmar Contraseña") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Ícono de candado") },
            trailingIcon = { if (viewModel.confirmPasswordError.value != null) Icon(Icons.Filled.Warning, "Error", tint = MaterialTheme.colorScheme.error) },
            isError = viewModel.confirmPasswordError.value != null,
            supportingText = { if (viewModel.confirmPasswordError.value != null) Text(viewModel.confirmPasswordError.value!!) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            // 2. LE PASAMOS EL CONTEXTO A LA FUNCIÓN REGISTRAR
            onClick = { viewModel.registrar(context = context, onSuccess = onNavigateToLogin) },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Registrarse")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegistroScreenPreview() {
    SanaesenciaTheme {
        RegistroScreen(
            onNavigateToLogin = {}
        )
    }
}
