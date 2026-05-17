package com.duoji.app.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.DuoJiApplication
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ManualRecordUiState(
    val type: String = "expense",
    val amountText: String = "",
    val category: String = "餐饮",
    val occurredAt: LocalDate = LocalDate.now(),
    val note: String = "",
    val merchantOrItem: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
)

class ManualRecordViewModel : ViewModel() {

    private val repository: TransactionRepository =
        DuoJiApplication.instance.container.transactionRepository

    private val _uiState = MutableStateFlow(ManualRecordUiState())
    val uiState: StateFlow<ManualRecordUiState> = _uiState.asStateFlow()

    private val expenseCategories = listOf("餐饮", "交通", "购物", "居住", "娱乐", "学习", "医疗", "通讯", "人情", "旅行", "其他")
    private val incomeCategories = listOf("工资", "副业", "红包", "退款", "其他收入")

    fun updateType(type: String) {
        val defaultCategory = when (type) {
            "income" -> "工资"
            "expense" -> "餐饮"
            else -> "其他"
        }
        _uiState.value = _uiState.value.copy(
            type = type,
            category = defaultCategory,
            errorMessage = null
        )
    }

    fun updateAmount(amount: String) {
        // Only allow positive numbers with optional decimal point
        val filtered = amount.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _uiState.value = _uiState.value.copy(amountText = filtered, errorMessage = null)
        }
    }

    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun updateDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(occurredAt = date)
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun updateMerchantOrItem(value: String) {
        _uiState.value = _uiState.value.copy(merchantOrItem = value)
    }

    fun save() {
        val s = _uiState.value

        if (s.amountText.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "先填一下金额吧。")
            return
        }

        val amount = s.amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "金额需要大于 0。")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

        val now = System.currentTimeMillis()
        val occurredAtMillis = s.occurredAt
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val entity = TransactionEntity(
            id = java.util.UUID.randomUUID().toString(),
            type = s.type,
            amount = amount,
            currency = "CNY",
            category = s.category,
            subcategory = null,
            note = s.note,
            merchantOrItem = s.merchantOrItem.ifBlank { null },
            occurredAt = occurredAtMillis,
            occurredAtText = null,
            source = "manual_form",
            rawText = null,
            confidence = 1.0,
            needUserConfirm = false,
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            try {
                repository.saveTransaction(entity)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "保存失败，请稍后再试。"
                )
            }
        }
    }

    fun getCategoriesForCurrentType(): List<String> {
        return when (_uiState.value.type) {
            "income" -> incomeCategories
            else -> expenseCategories
        }
    }
}
