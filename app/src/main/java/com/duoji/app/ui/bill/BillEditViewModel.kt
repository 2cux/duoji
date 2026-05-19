package com.duoji.app.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.DuoJiApplication
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class EditTransactionState(
    val isLoading: Boolean = true,
    val id: String = "",
    val type: String = "expense",
    val amountText: String = "",
    val amount: Double? = null,
    val currency: String = "CNY",
    val category: String = "",
    val note: String = "",
    val merchantOrItem: String = "",
    val occurredAtMillis: Long = System.currentTimeMillis(),
    val source: String = "",
    val confidence: Double = 1.0,
    val needUserConfirm: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val loadError: String? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: String? = null,
    val deleted: Boolean = false
)

class BillEditViewModel : ViewModel() {

    private val repository: TransactionRepository =
        DuoJiApplication.instance.container.transactionRepository

    private val _state = MutableStateFlow(EditTransactionState())
    val state: StateFlow<EditTransactionState> = _state.asStateFlow()

    fun loadTransaction(id: String) {
        viewModelScope.launch {
            repository.getTransactionById(id).collect { entity ->
                if (entity != null) {
                    _state.value = EditTransactionState(
                        isLoading = false,
                        id = entity.id,
                        type = entity.type,
                        amountText = formatAmountForEdit(entity.amount),
                        amount = entity.amount,
                        currency = entity.currency,
                        category = entity.category,
                        note = entity.note,
                        merchantOrItem = entity.merchantOrItem ?: "",
                        occurredAtMillis = entity.occurredAt,
                        source = entity.source,
                        confidence = entity.confidence,
                        needUserConfirm = entity.needUserConfirm,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                } else {
                    if (_state.value.deleted) {
                        _state.value = _state.value.copy(isLoading = false)
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            loadError = "账单未找到"
                        )
                    }
                }
            }
        }
    }

    fun updateAmount(text: String) {
        _state.value = _state.value.copy(
            amountText = text,
            amount = text.toDoubleOrNull()
        )
    }

    fun updateType(type: String) {
        _state.value = _state.value.copy(type = type)
    }

    fun updateCategory(category: String) {
        _state.value = _state.value.copy(category = category)
    }

    fun updateNote(note: String) {
        _state.value = _state.value.copy(note = note)
    }

    fun updateMerchantOrItem(value: String) {
        _state.value = _state.value.copy(merchantOrItem = value)
    }

    fun updateOccurredAt(date: LocalDate) {
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        _state.value = _state.value.copy(occurredAtMillis = millis)
    }

    fun save() {
        val s = _state.value
        if (s.amount == null) {
            _state.value = s.copy(saveError = "请填写金额")
            return
        }
        _state.value = s.copy(isSaving = true)

        viewModelScope.launch {
            try {
                repository.updateTransaction(
                    TransactionEntity(
                        id = s.id,
                        type = s.type,
                        amount = s.amount!!,
                        currency = s.currency,
                        category = s.category,
                        subcategory = null,
                        note = s.note,
                        merchantOrItem = s.merchantOrItem.ifBlank { null },
                        occurredAt = s.occurredAtMillis,
                        occurredAtText = null,
                        source = s.source,
                        rawText = null,
                        confidence = s.confidence,
                        needUserConfirm = s.needUserConfirm,
                        createdAt = s.createdAt,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveError = "保存失败，请稍后再试"
                )
            }
        }
    }

    fun delete() {
        val s = _state.value
        if (s.isDeleting) return
        _state.value = s.copy(isDeleting = true, deleteError = null)

        viewModelScope.launch {
            try {
                repository.deleteTransactionById(_state.value.id)
                _state.value = _state.value.copy(
                    isDeleting = false,
                    deleteSuccess = true,
                    deleted = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isDeleting = false,
                    deleteError = "删除失败，请重试"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(saveError = null)
    }

    fun clearDeleteError() {
        _state.value = _state.value.copy(deleteError = null)
    }

    fun categoriesForType(type: String): List<String> {
        return if (type == "income") {
            listOf("工资", "副业", "红包", "退款", "其他收入")
        } else {
            listOf("餐饮", "交通", "购物", "居住", "娱乐", "学习", "医疗", "通讯", "人情", "旅行", "其他")
        }
    }

    companion object {
        private fun formatAmountForEdit(amount: Double): String {
            return if (amount == amount.toLong().toDouble()) {
                amount.toLong().toString()
            } else {
                amount.toString()
            }
        }
    }
}
