package com.example.scansheet.upload

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.scansheet.GridPattern
import com.example.scansheet.darkBlue
import com.example.scansheet.mediumBlue
import com.example.scansheet.request.UploadResult

// Cores do gradiente principal
private val primaryGradient = listOf(
    Color(0xFF263A73), // Color(red: 0.15, green: 0.25, blue: 0.45)
    Color(0xFF405A8C)  // Color(red: 0.25, green: 0.35, blue: 0.55)
)

// Cores do gradiente do botão
private val buttonGradient = listOf(
    Color(0xCC1E88E5), // Color.blue.opacity(0.8)
    Color(0x9900BCD4)  // Color.cyan.opacity(0.6)
)

@Composable
fun UploadStepView(
    viewModel: UploadStepViewModel = viewModel(),
    onPickerClick: (PickerTarget) -> Unit,
    onNavigateToResults: () -> Unit // Função de navegação para sucesso
) {
    val imageUriA by viewModel.imageUriA.collectAsState()
    val imageUriB by viewModel.imageUriB.collectAsState()
    val isReadyToUpload by viewModel.isReadyToUpload.collectAsState()
    val uploadResult by viewModel.uploadResult.collectAsState()

    uploadResult?.let { result ->
        UploadStatusDialog(
            result = result,
            onDismiss = { viewModel.dismissUploadResult() },
            onNavigate = onNavigateToResults
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Gradiente de fundo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(colors = listOf(darkBlue, mediumBlue))
                )
        )

        GridPattern(modifier = Modifier.fillMaxSize().alpha(0.3f))

        // Conteúdo principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Seção do cabeçalho
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Ícone do app
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = buttonGradient,
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(35.dp),
                        tint = Color.White
                    )
                }

                // Título e subtítulo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Upload de Imagens",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Adicione duas imagens da ficha para processamento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Seção dos Cards de Upload
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Label da seção
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Selecione as imagens",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Cards de upload de imagem
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ImageUploadCard(
                        title = "Imagem 1",
                        imageUri = imageUriA,
                        hasImage = imageUriA != null,
                        action = { onPickerClick(PickerTarget.A) }
                    )

                    // Card para a segunda imagem
                    ImageUploadCard(
                        title = "Imagem 2",
                        imageUri = imageUriB,
                        hasImage = imageUriB != null,
                        action = { onPickerClick(PickerTarget.B) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botão de envio
            val buttonAlpha by animateFloatAsState(
                targetValue = if (isReadyToUpload) 1f else 0.6f,
                animationSpec = tween(durationMillis = 300),
                label = "buttonAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = if (isReadyToUpload) 15.dp else 5.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black.copy(alpha = if (isReadyToUpload) 0.3f else 0.1f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = if (isReadyToUpload) {
                            Brush.linearGradient(
                                colors = buttonGradient,
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Gray.copy(alpha = 0.6f),
                                    Color.Gray.copy(alpha = 0.4f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        }
                    )
                    .clickable(enabled = isReadyToUpload) {
                        viewModel.createUploadRequest()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White.copy(alpha = buttonAlpha)
                    )

                    Text(
                        text = "Enviar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = buttonAlpha)
                    )
                }
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

/**
 * Card de upload de imagem
 */
@Composable
private fun ImageUploadCard(
    title: String,
    imageUri: Uri?,
    hasImage: Boolean,
    action: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = Color.White.copy(alpha = if (hasImage) 0.15f else 0.1f)
            )
            .border(
                width = if (hasImage) 2.dp else 1.dp,
                color = Color.White.copy(alpha = if (hasImage) 0.4f else 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { action() }
            .padding(20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview da imagem ou placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagem selecionada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Título e status
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Text(
                    text = if (hasImage) "Imagem carregada" else "Toque para adicionar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = if (hasImage) 0.9f else 0.6f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Indicador de status
            if (hasImage) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Green
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}


/**
 * O Dialog que oferece as opções de Câmera ou Galeria. (Sem alterações)
 */
@Composable
fun SourceChoiceDialog(
    onDismissRequest: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Escolha uma opção") },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("De onde você quer adicionar a imagem?")

                ChoiceRow(
                    text = "Tirar foto",
                    icon = Icons.Default.Add,
                    onClick = onCameraClick
                )
                ChoiceRow(
                    text = "Escolher da galeria",
                    icon = Icons.Default.Add,
                    onClick = onGalleryClick
                )
            }
        }
    )
}

@Composable
private fun ChoiceRow(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun UploadStatusDialog(
    result: UploadResult,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            // Impede o fechamento do diálogo de carregamento ao clicar fora
            if (result !is UploadResult.Loading) {
                onDismiss()
            }
        },
        confirmButton = {
            when (result) {
                is UploadResult.Success -> {
                    TextButton(onClick = onNavigate) {
                        Text("Ver Resultados")
                    }
                }
                is UploadResult.Failure -> {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar")
                    }
                }
                else -> {} // Sem botão no estado de carregamento
            }
        },
        title = {
            val titleText = when (result) {
                is UploadResult.Loading -> "Enviando Imagens"
                is UploadResult.Success -> "Sucesso!"
                is UploadResult.Failure -> "Erro no Upload"
            }
            Text(text = titleText)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (result) {
                    is UploadResult.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Aguarde, estamos processando a ficha...",
                            textAlign = TextAlign.Center
                        )
                    }
                    is UploadResult.Success -> {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Sucesso",
                            tint = Color(0xFF4CAF50), // Verde
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "O arquivo foi processado e salvo com sucesso.",
                            textAlign = TextAlign.Center
                        )
                    }
                    is UploadResult.Failure -> {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Falha",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = result.message,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        // Estilização para combinar com a UI
        containerColor = Color(0xFF2C3E50), // Um azul escuro
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.8f)
    )
}