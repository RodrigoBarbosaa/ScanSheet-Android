package com.example.scansheet
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ExportResultsScreen(
    viewModel: ExportResultsViewModel = viewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    // Activity result launcher para compartilhamento
    val shareFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Limpar seleção após compartilhamento
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.clearSelection()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadCSVFiles(context.contentResolver)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(colors = listOf(darkBlue, mediumBlue))
            )
    ) {
        GridPattern(modifier = Modifier.fillMaxSize().alpha(0.3f))

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            HeaderView(
                filesCount = uiState.csvFiles.size,
                // --- MUDANÇA AQUI ---
                // Removido o padding lateral (start = 20.dp)
                modifier = Modifier.padding(top = 50.dp)
            )

            when {
                uiState.isLoading -> {
                    LoadingView(
                        modifier = Modifier.weight(1f)
                    )
                }

                uiState.csvFiles.isEmpty() -> {
                    EmptyStateView(
                        onScanClick = { navController.navigate("ficha_selection_screen") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 40.dp)
                    )
                }

                else -> {
                    // Files list
                    FilesListView(
                        files = uiState.csvFiles,
                        selectedFiles = uiState.selectedFiles,
                        onToggleSelection = viewModel::toggleFileSelection,
                        modifier = Modifier.weight(1f)
                    )

                    // Bottom actions
                    BottomActionsView(
                        selectedCount = uiState.selectedFiles.size,
                        isProcessing = uiState.isProcessing,
                        onDeleteClick = viewModel::showDeleteDialog,
                        onShareClick = {
                            scope.launch {
                                val intent = createShareIntent(uiState.selectedFiles.toList())
                                shareFilesLauncher.launch(intent)
                            }
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteDialog,
            title = { Text("Confirmar Exclusão") },
            text = {
                Text("Deseja excluir ${uiState.selectedFiles.size} arquivo(s) selecionado(s)? Esta ação não pode ser desfeita.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedFiles(context.contentResolver)
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error dialog
    if (uiState.showErrorDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideErrorDialog,
            title = { Text("Erro") },
            text = { Text(uiState.errorMessage) },
            confirmButton = {
                TextButton(onClick = viewModel::hideErrorDialog) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun HeaderView(
    filesCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "Arquivos Salvos",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (filesCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$filesCount arquivo(s) encontrado(s)",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun LoadingView(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Carregando arquivos...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun EmptyStateView(
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        // Icon container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .background(
                    Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(35.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Nenhum arquivo encontrado",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Escaneie suas primeiras planilhas para começar a gerenciar seus arquivos CSV",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onScanClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .shadow(10.dp, RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Blue.copy(alpha = 0.8f),
                            Color.Cyan.copy(alpha = 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = "Escanear Planilha",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun FilesListView(
    files: List<CSVFileInfo>,
    selectedFiles: Set<CSVFileInfo>,
    onToggleSelection: (CSVFileInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(files) { file ->
            CSVFileRow(
                fileInfo = file,
                isSelected = selectedFiles.contains(file),
                onToggleSelection = { onToggleSelection(file) }
            )
        }
    }
}

@Composable
private fun CSVFileRow(
    fileInfo: CSVFileInfo,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggleSelection() }
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (isSelected) 0.15f else 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color.Blue.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Checkbox
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isSelected) Color.Blue.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // File icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Green.copy(alpha = 0.7f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // File info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fileInfo.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = formatFileSize(fileInfo.size),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Text(
                        text = formatDate(fileInfo.dateCreated),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActionsView(
    selectedCount: Int,
    isProcessing: Boolean,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 40.dp)
    ) {
        if (selectedCount > 0) {
            Text(
                text = "$selectedCount arquivo(s) selecionado(s)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Delete button
            Button(
                onClick = onDeleteClick,
                enabled = selectedCount > 0 && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = if (selectedCount > 0) 0.7f else 0.3f),
                    disabledContainerColor = Color.Red.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = if (selectedCount > 0) 8.dp else 2.dp,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = "Delete",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Share button
            Button(
                onClick = onShareClick,
                enabled = selectedCount > 0 && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = if (selectedCount > 0) 8.dp else 2.dp,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(
                        brush = if (selectedCount > 0) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Green.copy(alpha = 0.7f),
                                    Color(0xFF00E5FF).copy(alpha = 0.6f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Gray.copy(alpha = 0.3f),
                                    Color.Gray.copy(alpha = 0.3f)
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = "Share",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Data classes e funções auxiliares
data class CSVFileInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateCreated: Long
)

private fun formatFileSize(sizeBytes: Long): String {
    val kb = sizeBytes / 1024.0
    val mb = kb / 1024.0

    return when {
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$sizeBytes B"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun createShareIntent(files: List<CSVFileInfo>): Intent {
    return if (files.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, files.first().uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files.map { it.uri }))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}