package com.example.scansheet

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scansheet.CSVFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExportResultsUiState(
    val isLoading: Boolean = true,
    val csvFiles: List<CSVFileInfo> = emptyList(),
    val selectedFiles: Set<CSVFileInfo> = emptySet(),
    val isProcessing: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showErrorDialog: Boolean = false,
    val errorMessage: String = ""
)

class ExportResultsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ExportResultsUiState())
    val uiState: StateFlow<ExportResultsUiState> = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.Q)
    fun loadCSVFiles(contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val files = withContext(Dispatchers.IO) {
                    // Primeiro tenta buscar pelos Downloads
                    var csvFiles = getCSVFilesFromDownloads(contentResolver)

                    // Se não encontrar nada, tenta uma busca mais ampla
                    if (csvFiles.isEmpty()) {
                        println("Tentando busca alternativa...")
                        csvFiles = getCSVFilesAlternativeSearch(contentResolver)
                    }

                    csvFiles
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    csvFiles = files
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showErrorDialog = true,
                    errorMessage = "Erro ao carregar arquivos: ${e.message}"
                )
            }
        }
    }

    // Busca alternativa - procura por todos os arquivos CSV no dispositivo
    private suspend fun getCSVFilesAlternativeSearch(contentResolver: ContentResolver): List<CSVFileInfo> {
        val csvFiles = mutableListOf<CSVFileInfo>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )

        // Busca mais ampla - todos os arquivos CSV
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%.csv")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                println("Busca alternativa encontrou ${cursor.count} arquivos")

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "unknown.csv"
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn) * 1000

                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Files.getContentUri("external"),
                        id.toString()
                    )

                    csvFiles.add(
                        CSVFileInfo(
                            uri = contentUri,
                            name = name,
                            size = size,
                            dateCreated = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("Erro na busca alternativa: ${e.message}")
        }

        return csvFiles
    }

    fun toggleFileSelection(file: CSVFileInfo) {
        val currentSelection = _uiState.value.selectedFiles.toMutableSet()

        if (currentSelection.contains(file)) {
            currentSelection.remove(file)
        } else {
            currentSelection.add(file)
        }

        _uiState.value = _uiState.value.copy(selectedFiles = currentSelection)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedFiles = emptySet())
    }

    fun showDeleteDialog() {
        if (_uiState.value.selectedFiles.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(showDeleteDialog = true)
        }
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }

    fun hideErrorDialog() {
        _uiState.value = _uiState.value.copy(
            showErrorDialog = false,
            errorMessage = ""
        )
    }

    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(
            showErrorDialog = true,
            errorMessage = message
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteSelectedFiles(contentResolver: ContentResolver) {
        val filesToDelete = _uiState.value.selectedFiles
        if (filesToDelete.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                showDeleteDialog = false
            )

            try {
                withContext(Dispatchers.IO) {
                    filesToDelete.forEach { file ->
                        try {
                            contentResolver.delete(file.uri, null, null)
                        } catch (e: Exception) {
                            // Log individual file deletion errors
                            println("Erro ao excluir ${file.name}: ${e.message}")
                        }
                    }
                }

                // Recarregar arquivos e limpar seleção
                _uiState.value = _uiState.value.copy(
                    selectedFiles = emptySet(),
                    isProcessing = false
                )

                // Recarregar lista de arquivos
                loadCSVFiles(contentResolver)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    showErrorDialog = true,
                    errorMessage = "Erro ao excluir arquivos: ${e.message}"
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun getCSVFilesFromDownloads(contentResolver: ContentResolver): List<CSVFileInfo> {
        val csvFiles = mutableListOf<CSVFileInfo>()

        // Buscar em Downloads usando MediaStore.Downloads
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.SIZE,
            MediaStore.Downloads.DATE_ADDED
        )

        // Buscar arquivos .csv nos Downloads
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%.csv")
        val sortOrder = "${MediaStore.Downloads.DATE_ADDED} DESC"

        try {
            // Primeira tentativa: MediaStore.Downloads (Android 10+)
            contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)

                println("Encontrados ${cursor.count} registros no MediaStore.Downloads")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "unknown.csv"
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn) * 1000

                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    println("Arquivo encontrado: $name (URI: $contentUri)")

                    csvFiles.add(
                        CSVFileInfo(
                            uri = contentUri,
                            name = name,
                            size = size,
                            dateCreated = dateAdded
                        )
                    )
                }
            }

            // Se não encontrou nada no Downloads, tentar buscar em Files
            if (csvFiles.isEmpty()) {
                println("Nenhum arquivo encontrado no Downloads, tentando Files...")

                val filesProjection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.RELATIVE_PATH
                )

                val filesSelection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? AND " +
                        "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
                val filesSelectionArgs = arrayOf("%.csv", "%Download%")

                contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    filesProjection,
                    filesSelection,
                    filesSelectionArgs,
                    sortOrder
                )?.use { cursor ->
                    println("Encontrados ${cursor.count} registros no MediaStore.Files")

                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: "unknown.csv"
                        val size = cursor.getLong(sizeColumn)
                        val dateAdded = cursor.getLong(dateColumn) * 1000

                        val contentUri = Uri.withAppendedPath(
                            MediaStore.Files.getContentUri("external"),
                            id.toString()
                        )

                        println("Arquivo encontrado em Files: $name (URI: $contentUri)")

                        csvFiles.add(
                            CSVFileInfo(
                                uri = contentUri,
                                name = name,
                                size = size,
                                dateCreated = dateAdded
                            )
                        )
                    }
                }
            }

        } catch (e: Exception) {
            println("Erro ao buscar arquivos CSV: ${e.message}")
            e.printStackTrace()
            throw e
        }

        println("Total de arquivos CSV encontrados: ${csvFiles.size}")
        return csvFiles
    }
}