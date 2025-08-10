package com.example.scansheet.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun UploadScreen(navController: NavController) {

    val viewModel: UploadStepViewModel = viewModel()
    var activePickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    var showCameraView by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            activePickerTarget?.let { target ->
                viewModel.updateImageUri(target, uri)
            }
            activePickerTarget = null
        }
    )

    if (showCameraView) {
        CameraView(
            onImageCaptured = { uri ->
                activePickerTarget?.let { target ->
                    viewModel.updateImageUri(target, uri)
                }
                showCameraView = false // Volta para a tela principal
                activePickerTarget = null
            },
            onError = { exception ->
                println("Erro na câmera: ${exception.message}")
                showCameraView = false // Volta para a tela principal em caso de erro
                activePickerTarget = null
            }
        )
    } else {
        // Exibe o dialog apenas quando um alvo está ativo
        if (activePickerTarget != null) {
            SourceChoiceDialog(
                onDismissRequest = { activePickerTarget = null },
                onCameraClick = {
                    showCameraView = true // Navega para a tela da câmera
                    // O dialog será fechado no onImageCaptured da CameraView
                },
                onGalleryClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    // O dialog será fechado no onResult do galleryLauncher
                }
            )
        }

        // A UI principal
        UploadStepView(
            viewModel = viewModel,
            onPickerClick = { target ->
                activePickerTarget = target
            },
            onNavigateToResults = {
                navController.navigate("export_results_screen")
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UploadPreview() {
    // Para o preview, podemos passar um NavController vazio, pois não haverá navegação real.
    UploadScreen(navController = rememberNavController())
}