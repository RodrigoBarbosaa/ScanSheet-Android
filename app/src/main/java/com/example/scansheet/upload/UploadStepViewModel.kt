package com.example.scansheet.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadStepViewModel : ViewModel() {

    private val _imageUriA = MutableStateFlow<Uri?>(null)
    val imageUriA = _imageUriA.asStateFlow()

    private val _imageUriB = MutableStateFlow<Uri?>(null)
    val imageUriB = _imageUriB.asStateFlow()

    // O botão de upload é ativado quando ambas as URIs não são nulas
    val isReadyToUpload = combine(_imageUriA, _imageUriB) { uriA, uriB ->
        uriA != null && uriB != null
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    // Função única para atualizar a URI baseada no alvo (A ou B)
    fun updateImageUri(target: PickerTarget, uri: Uri?) {
        when (target) {
            PickerTarget.A -> _imageUriA.update { uri }
            PickerTarget.B -> _imageUriB.update { uri }
        }
    }

    fun createUploadRequest() {
        viewModelScope.launch {
            val uriA = _imageUriA.value ?: return@launch
            val uriB = _imageUriB.value ?: return@launch

            // TODO: Implementar a lógica de criação da requisição de upload.
            println("Pronto para criar requisição com:")
            println("URI A: $uriA")
            println("URI B: $uriB")
        }
    }
}
enum class PickerTarget {
    A, B
}