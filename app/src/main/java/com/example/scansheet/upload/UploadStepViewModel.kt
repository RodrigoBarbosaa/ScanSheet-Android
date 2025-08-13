package com.example.scansheet.upload

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scansheet.request.Request
import com.example.scansheet.request.UploadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadStepViewModel(application: Application) : AndroidViewModel(application) {

    private val uploadService = Request(application.applicationContext)

    // LiveData para observar o resultado da operação na sua UI
    private val _uploadResult = MutableStateFlow<UploadResult?>(null)
    val uploadResult = _uploadResult.asStateFlow()

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

    fun createUploadRequest(tag: String) {
        if (!isReadyToUpload.value) return

        viewModelScope.launch {
            _uploadResult.value = UploadResult.Loading

            val uris = listOfNotNull(_imageUriA.value, _imageUriB.value)
            _uploadResult.value = uploadService.createUploadRequest(uris, tag)
        }
    }
    fun dismissUploadResult() {
        _uploadResult.value = null
    }
}
enum class PickerTarget {
    A, B
}