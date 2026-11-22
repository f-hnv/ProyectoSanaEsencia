package com.example.sanaesencia.ui.perfil

import android.content.Context
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanaesencia.viewmodel.PerfilViewModel
import coil.compose.rememberAsyncImagePainter
import java.io.File
import androidx.compose.ui.tooling.preview.Preview
import com.example.sanaesencia.ui.theme.SanaesenciaTheme
import com.example.sanaesencia.ui.components.SanaOutlineButton as OutlinedButton
import com.example.sanaesencia.ui.components.SanaOutlineButton as ElevatedButton
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val teclado=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val foco=androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current
    val fotoUri by viewModel.fotoPerfilUri
    var nombre by rememberSaveable { mutableStateOf("") }
    var apellido by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(viewModel.perfil?.email) {
        nombre = com.example.sanaesencia.data.remote.SesionBackend.actual()?.usuario?.nombre.orEmpty()
        apellido = com.example.sanaesencia.data.remote.SesionBackend.actual()?.usuario?.apellido.orEmpty()
        telefono = viewModel.perfil?.telefono.orEmpty()
    }
    var tempCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var errorRecurso by remember { mutableStateOf<String?>(null) }

    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> viewModel.actualizarFotoPerfil(uri) }

    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.actualizarFotoPerfil(tempCameraUri?.let(Uri::parse))
    }

    fun abrirCamara() {
        errorRecurso = null
        try {
            val uri = createTempPictureUri(context)
            tempCameraUri = uri.toString()
            camaraLauncher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            errorRecurso = "No hay una aplicación de cámara disponible. Puedes elegir una imagen de la galería."
        } catch (_: SecurityException) {
            errorRecurso = "No se pudo acceder a la cámara. Revisa el permiso o usa la galería."
        }
    }

    val permisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara()
        else errorRecurso = "No se concedió el permiso de cámara. Puedes continuar y elegir una imagen de la galería."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                if (fotoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(fotoUri),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Sin foto",
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(viewModel.perfil?.email.orEmpty(),style=MaterialTheme.typography.bodyMedium)
            com.example.sanaesencia.data.remote.SesionBackend.actual()?.usuario?.let {
                Text("Número de cuenta: ${it.id}", style=MaterialTheme.typography.bodySmall)
                Text("RUN: ${it.run}", style=MaterialTheme.typography.bodySmall)
            }
            Text("Perfil: ${viewModel.perfil?.rol?.lowercase() ?: "sin sesión"}",style=MaterialTheme.typography.labelMedium)
            OutlinedTextField(nombre,{nombre=it},label={Text("Nombre")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(apellido,{apellido=it},label={Text("Apellido")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(telefono,{telefono=it},label={Text("Teléfono de contacto")},modifier=Modifier.fillMaxWidth(),singleLine=true,
                keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Phone))
            ElevatedButton(onClick={teclado?.hide();foco.clearFocus();viewModel.guardar(nombre,telefono,apellido)},enabled=!viewModel.guardando && viewModel.perfil!=null,
                modifier=Modifier.fillMaxWidth()){Text(if(viewModel.guardando) "Guardando…" else "Guardar perfil")}
            viewModel.mensaje?.let { Text(it,modifier=Modifier.padding(vertical=8.dp)) }
            Text("El nombre y apellido se guardan en tu cuenta. El teléfono y la foto se conservan en este dispositivo.",style=MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ElevatedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                            abrirCamara()
                        else permisoCamara.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Cámara", modifier = Modifier.padding(end = 8.dp))
                    Text("Cámara")
                }

                ElevatedButton(
                    onClick = {
                        errorRecurso = null
                        try {
                            galeriaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } catch (_: ActivityNotFoundException) {
                            errorRecurso = "No hay un selector de imágenes disponible en el dispositivo."
                        }
                    }
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Galería", modifier = Modifier.padding(end = 8.dp))
                    Text("Galería")
                }
            }

            errorRecurso?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }

            Spacer(modifier = Modifier.height(48.dp))

            // Botón de cerrar sesión
            OutlinedButton(
                onClick = {
                    com.example.sanaesencia.data.repository.CuentaRepository(context).salir()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = "Cerrar Sesión", modifier = Modifier.padding(end = 8.dp))
                Text("Cerrar Sesión")
            }
        }
    }
}

fun createTempPictureUri(context: Context): Uri {
    val tempFile = File.createTempFile("perfil_camera_", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        tempFile
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PerfilScreenPreview() {
    SanaesenciaTheme {
        PerfilScreen(onNavigateBack = {}, onLogout = {})
    }
}

