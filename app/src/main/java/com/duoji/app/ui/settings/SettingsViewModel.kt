package com.duoji.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.DuoJiApplication
import com.duoji.app.data.export.ExportService
import com.duoji.app.data.repository.TransactionRepository
import com.duoji.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiBaseUrl: String = "https://api.deepseek.com",
    val apiKey: String = "",
    val modelName: String = "deepseek-v4-flash",
    val useRealAI: Boolean = false,
    val useWarmReminder: Boolean = true,
    val monthlyBudget: Double = -1.0,
    val exportMessage: String? = null,
    val errorMessage: String? = null,
    val isExporting: Boolean = false,
    val isClearing: Boolean = false
)

class SettingsViewModel : ViewModel() {

    private val settingsRepository: SettingsRepository =
        DuoJiApplication.instance.container.settingsRepository

    private val transactionRepository: TransactionRepository =
        DuoJiApplication.instance.container.transactionRepository

    private val exportService = ExportService(DuoJiApplication.instance)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            _uiState.value = _uiState.value.copy(
                apiBaseUrl = settings.apiBaseUrl,
                apiKey = settings.apiKey,
                modelName = settings.modelName,
                useRealAI = settings.useRealAI,
                useWarmReminder = settings.useWarmReminder,
                monthlyBudget = settings.monthlyBudget
            )
        }
    }

    fun updateApiBaseUrl(value: String) {
        _uiState.value = _uiState.value.copy(apiBaseUrl = value)
    }

    fun updateApiKey(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value)
    }

    fun updateModelName(value: String) {
        _uiState.value = _uiState.value.copy(modelName = value)
    }

    fun updateUseRealAI(value: Boolean) {
        _uiState.value = _uiState.value.copy(useRealAI = value)
    }

    fun updateWarmReminder(value: Boolean) {
        _uiState.value = _uiState.value.copy(useWarmReminder = value)
    }

    fun saveAISettings() {
        viewModelScope.launch {
            val s = _uiState.value
            settingsRepository.saveAISettings(
                apiBaseUrl = s.apiBaseUrl,
                apiKey = s.apiKey,
                modelName = s.modelName,
                useRealAI = s.useRealAI
            )
            _uiState.value = _uiState.value.copy(
                exportMessage = "AI 设置已保存。"
            )
        }
    }

    fun updateAndSaveWarmReminder(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useWarmReminder = enabled)
        viewModelScope.launch {
            settingsRepository.saveWarmReminder(enabled)
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null, errorMessage = null)
            try {
                val transactions = transactionRepository.getAllTransactionsOnce()
                if (transactions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        errorMessage = "还没有账单，先记几笔再来导出吧。"
                    )
                    return@launch
                }
                val result = exportService.exportCsv(transactions)
                result.fold(
                    onSuccess = { msg ->
                        _uiState.value = _uiState.value.copy(isExporting = false, exportMessage = msg)
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            errorMessage = "导出失败，请稍后再试。"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "导出失败，请稍后再试。"
                )
            }
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null, errorMessage = null)
            try {
                val transactions = transactionRepository.getAllTransactionsOnce()
                if (transactions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        errorMessage = "还没有账单，先记几笔再来导出吧。"
                    )
                    return@launch
                }
                val result = exportService.exportJson(transactions)
                result.fold(
                    onSuccess = { msg ->
                        _uiState.value = _uiState.value.copy(isExporting = false, exportMessage = msg)
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            errorMessage = "导出失败，请稍后再试。"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "导出失败，请稍后再试。"
                )
            }
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearing = true)
            try {
                transactionRepository.deleteAllTransactions()
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    exportMessage = "本地账本已清空。"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    errorMessage = "清空失败，请稍后再试。"
                )
            }
        }
    }

    fun saveMonthlyBudget(budget: Double) {
        viewModelScope.launch {
            settingsRepository.saveMonthlyBudget(budget)
            _uiState.value = _uiState.value.copy(
                monthlyBudget = budget,
                exportMessage = if (budget > 0) "本月预算已设置" else "本月预算已清除"
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null, errorMessage = null)
    }
}
