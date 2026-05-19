package com.duoji.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.DuoJiApplication
import com.duoji.app.data.model.TransactionDraft
import com.duoji.app.data.repository.AIRepository
import com.duoji.app.data.store.ParseResultStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecordUiState(
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null,
    val parsedSuccessfully: Boolean = false,
    val usingLocalFallback: Boolean = false,
    val localFallbackReason: String? = null
)

class RecordViewModel : ViewModel() {

    private val repository = AIRepository(
        DuoJiApplication.instance.container.settingsRepository
    )

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(
            inputText = text,
            error = null
        )
    }

    fun process() {
        val input = _uiState.value.inputText.trim()
        if (input.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入记账内容")
            return
        }

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            error = null,
            parsedSuccessfully = false
        )

        viewModelScope.launch {
            val result = repository.parse(input)

            result.fold(
                onSuccess = { drafts ->
                    ParseResultStore.drafts = drafts
                    ParseResultStore.localFallbackReason = repository.lastFallbackReason
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        parsedSuccessfully = true,
                        usingLocalFallback = repository.lastFallbackReason != null,
                        localFallbackReason = repository.lastFallbackReason
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = error.message ?: "识别失败，可手动记账",
                        parsedSuccessfully = false
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
