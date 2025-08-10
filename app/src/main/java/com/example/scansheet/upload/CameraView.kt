package com.example.scansheet.upload

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

//================================================================================
// 1. O Composable da Câmera
//================================================================================

/**
 * Uma tela de câmera full-screen que permite ao usuário tirar uma foto.
 *
 * @param onImageCaptured Callback invocado com a URI da imagem salva com sucesso.
 * @param onError Callback invocado se ocorrer um erro.
 */
@Composable
fun CameraView(
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val context = LocalContext.current
    var hasCamPermission by remember { mutableStateOf(false) }
    // Novo estado para armazenar a URI da imagem capturada para o preview
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCamPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCamPermission) {
            // Se não houver imagem capturada, mostra a câmera. Senão, mostra o preview.
            if (capturedImageUri == null) {
                CameraPreview(
                    onImageCaptured = { uri ->
                        // Atualiza o estado para mostrar o preview
                        capturedImageUri = uri
                    },
                    onError = onError
                )
            } else {
                ImagePreview(
                    uri = capturedImageUri!!,
                    onConfirm = {
                        // Confirma a imagem e continua o fluxo original
                        onImageCaptured(capturedImageUri!!)
                    },
                    onCancel = {
                        // Cancela e volta para a câmera, limpando a URI
                        capturedImageUri = null
                    }
                )
            }
        }
    }
}

/**
 * Composable que mostra a pré-visualização da câmera e o botão de captura.
 */
@Composable
private fun CameraPreview(
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }.also { previewView ->
                    startCamera(
                        context = context,
                        lifecycleOwner = lifecycleOwner,
                        cameraController = cameraController,
                        previewView = previewView
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = {
                val executor = ContextCompat.getMainExecutor(context)
                takePicture(
                    cameraController = cameraController,
                    executor = executor,
                    context = context,
                    onImageCaptured = onImageCaptured,
                    onError = onError
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp) // Padding inferior para não sobrepor a nav bar
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "Tirar foto",
                tint = Color.White,
                modifier = Modifier
                    .size(64.dp)
                    .padding(1.dp)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Composable que mostra a imagem capturada com botões de confirmar e cancelar.
 */
@Composable
private fun ImagePreview(
    uri: Uri,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = uri,
            contentDescription = "Preview da imagem capturada",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                // Padding ajustado para elevar os botões
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelar",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            IconButton(onClick = onConfirm) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Confirmar",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}


private fun takePicture(
    cameraController: ImageCapture,
    executor: Executor,
    context: Context,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = createImageFile(context)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    cameraController.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.let(onImageCaptured)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

private fun startCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraController: ImageCapture,
    previewView: PreviewView
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                cameraController
            )
        } catch (exc: Exception) {
            println("Falha ao iniciar a câmera: ${exc.message}")
        }
    }, ContextCompat.getMainExecutor(context))
}

fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val imageDir = File(context.cacheDir, "images")
    if (!imageDir.exists()) imageDir.mkdirs()
    return File(imageDir, "JPEG_${timeStamp}.jpg")
}