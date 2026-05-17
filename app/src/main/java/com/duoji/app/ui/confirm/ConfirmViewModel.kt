package com.duoji.app.ui.confirm

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.data.model.TransactionDraft
import com.duoji.app.data.model.TransactionType
import com.duoji.app.data.store.ParseResultStore
import com.duoji.app.data.store.TransactionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfirmUiState(
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val deleteTargetIndex: Int? = null
)

class ConfirmViewModel : ViewModel() {

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
            // Shift errors for indices after the removed one
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
        // Validate: check for missing amounts
        _errors.clear()
        var hasError = false

        _transactions.forEachIndexed { index, draft ->
            if (draft.amount == null || (draft.amountText.isNotBlank() && draft.amount == null)) {
                _errors[index] = "请填写金额"
                hasError = true
            }
        }

        if (hasError || _transactions.isEmpty()) {
            return false
        }

        // Save all transactions
        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            _transactions.forEach { draft ->
                TransactionStore.save(draft)
            }
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = true
            )
        }

        return true
    }
}
