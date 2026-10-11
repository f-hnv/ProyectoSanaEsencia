package com.example.sanaesencia.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegistro: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val email = viewModel.email.value
    val emailError = viewModel.emailError.value
    val password = viewModel.password.value
    val passwordError = viewModel.passwordError.value

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.login(onSuccess = onNavigateToHome) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Ingresar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegistro) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
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

