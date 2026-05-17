package com.duoji.app.ui.confirm

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.DuoJiApplication
import com.duoji.app.data.model.TransactionDraft
import com.duoji.app.data.model.TransactionType
import com.duoji.app.data.store.ParseResultStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfirmUiState(
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val showDeleteConfirm: Boolean = false,
    val deleteTargetIndex: Int? = null
)

class ConfirmViewModel : ViewModel() {

    private val repository = DuoJiApplication.instance.container.transactionRepository

    private val _transactions = mutableStateListOf<TransactionDraft>()
    val transactions: List<TransactionDraft> get() = _transactions

    private val _errors = mutableStateMapOf<Int, String>()
    val errors: Map<Int, String> get() = _errors

    private val _uiState = MutableStateFlow(ConfirmUiState())
    val uiState: StateFlow<ConfirmUiState> = _uiState.asStateFlow()

    init {
        loadFromStore()
    }

    private fun loadFromStore() {
        _transactions.clear()
        _errors.clear()
        _transactions.addAll(ParseResultStore.drafts)
    }

    fun updateAmount(index: Int, text: String) {
        if (index < 0 || index >= _transactions.size) return
        val amount = text.toDoubleOrNull()
        _transactions[index] = _transactions[index].copy(
            amountText = text,
            amount = amount
        )
        if (amount != null) {
            _errors.remove(index)
        }
    }

    fun updateCategory(index: Int, category: String) {
        if (index < 0 || index >= _transactions.size) return
        _transactions[index] = _transactions[index].copy(category = category)
    }

    fun updateNote(index: Int, note: String) {
        if (index < 0 || index >= _transactions.size) return
        _transactions[index] = _transactions[index].copy(note = note)
    }

    fun updateOccurredAt(index: Int, dateTime: String) {
        if (index < 0 || index >= _transactions.size) return
        _transactions[index] = _transactions[index].copy(occurredAt = dateTime)
    }

    fun updateType(index: Int, type: TransactionType) {
        if (index < 0 || index >= _transactions.size) return
        _transactions[index] = _transactions[index].copy(type = type)
    }

    fun requestDelete(index: Int) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            deleteTargetIndex = index
        )
    }

    fun confirmDelete() {
        val index = _uiState.value.deleteTargetIndex ?: return
        if (index in _transactions.indices) {
            _transactions.removeAt(index)
            _errors.remove(index)
            val shiftedErrors = mutableMapOf<Int, String>()
            _errors.forEach { (idx, msg) ->
                val newIdx = if (idx > index) idx - 1 else idx
                shiftedErrors[newIdx] = msg
            }
            _errors.clear()
            _errors.putAll(shiftedErrors)
        }
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = false,
            deleteTargetIndex = null
        )
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = false,
            deleteTargetIndex = null
        )
    }

    fun confirmAll(): Boolean {
        _errors.clear()
        var hasError = false

        _transactions.forEachIndexed { index, draft ->
            if (draft.amount == null) {
                _errors[index] = "请填写金额"
                hasError = true
            }
        }

        if (hasError || _transactions.isEmpty()) {
            return false
        }

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                repository.saveDrafts(_transactions.toList())
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveError = "保存失败，请稍后再试"
                )
            }
        }

        return true
    }

    fun clearSaveError() {
        _uiState.value = _uiState.value.copy(saveError = null)
    }
}
