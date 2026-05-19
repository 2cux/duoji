package com.duoji.app.ui.statistics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.DuoJiApplication
import com.duoji.app.data.ai.MonthlyAdviceRepository
import com.duoji.app.data.repository.TransactionRepository
import com.duoji.app.domain.statistics.MonthlyAdviceState
import com.duoji.app.domain.statistics.MonthlyStatistics
import com.duoji.app.domain.statistics.StatisticsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TAG = "StatisticsVM"

data class StatisticsUiState(
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val statistics: MonthlyStatistics? = null,
    val adviceState: MonthlyAdviceState = MonthlyAdviceState(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class StatisticsViewModel : ViewModel() {

    private val repository: TransactionRepository =
        DuoJiApplication.instance.container.transactionRepository

    private val adviceRepository = MonthlyAdviceRepository(
        DuoJiApplication.instance.container.settingsRepository
    )

    private val useCase = StatisticsUseCase()

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        Log.d(TAG, "ViewModel init: loading current month")
        loadMonth(_uiState.value.selectedYear, _uiState.value.selectedMonth)
    }

    fun loadCurrentMonth() {
        val now = LocalDate.now()
        loadMonth(now.year, now.monthValue)
    }

    fun previousMonth() {
        try {
            val s = _uiState.value
            val date = LocalDate.of(s.selectedYear, s.selectedMonth, 1).minusMonths(1)
            loadMonth(date.year, date.monthValue)
        } catch (e: Exception) {
            Log.e(TAG, "previousMonth: date calc failed", e)
        }
    }

    fun nextMonth() {
        try {
            val s = _uiState.value
            val date = LocalDate.of(s.selectedYear, s.selectedMonth, 1)
            val now = LocalDate.now()
            val next = date.plusMonths(1)
            if (next.year > now.year || (next.year == now.year && next.monthValue > now.monthValue)) {
                return
            }
            loadMonth(next.year, next.monthValue)
        } catch (e: Exception) {
            Log.e(TAG, "nextMonth: date calc failed", e)
        }
    }

    private fun loadMonth(year: Int, month: Int) {
        Log.d(TAG, "loadMonth: year=$year month=$month")
        _uiState.value = _uiState.value.copy(
            selectedYear = year,
            selectedMonth = month,
            isLoading = true,
            errorMessage = null,
            adviceState = MonthlyAdviceState()
        )

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            try {
                Log.d(TAG, "loadMonth: creating flow for $year-$month")
                repository.observeTransactionsByMonth(year, month)
                    .catch { e ->
                        Log.e(TAG, "loadMonth: Flow error", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "加载统计数据失败：${e.localizedMessage ?: "未知错误"}"
                        )
                    }
                    .collect { transactions ->
                        try {
                            Log.d(TAG, "loadMonth: collected ${transactions.size} transactions")
                            val statistics = useCase.buildMonthlyStatistics(transactions, year, month)
                            Log.d(TAG, "loadMonth: statistics built: " +
                                    "txCount=${statistics.transactionCount}, " +
                                    "expense=${statistics.totalExpense}, " +
                                    "categories=${statistics.categorySummaries.size}, " +
                                    "dailySummaries=${statistics.dailySummaries.size}")
                            _uiState.value = _uiState.value.copy(
                                statistics = statistics,
                                isLoading = false,
                                errorMessage = null
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "loadMonth: buildMonthlyStatistics failed", e)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "统计数据生成失败：${e.localizedMessage ?: "未知错误"}"
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "loadMonth: unexpected exception before flow setup", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "加载失败：${e.localizedMessage ?: "未知错误"}"
                )
            }
        }
    }

    fun refresh() {
        Log.d(TAG, "refresh")
        loadMonth(_uiState.value.selectedYear, _uiState.value.selectedMonth)
    }

    fun generateMonthlyAdvice() {
        val stats = _uiState.value.statistics
        if (stats == null) {
            Log.w(TAG, "generateMonthlyAdvice: statistics is null, skipping")
            return
        }
        _uiState.value = _uiState.value.copy(
            adviceState = MonthlyAdviceState(isLoading = true)
        )

        viewModelScope.launch {
            try {
                Log.d(TAG, "generateMonthlyAdvice: starting...")
                val advice = adviceRepository.generateAdvice(stats)
                val cleaned = sanitizeMarkdown(advice)
                Log.d(TAG, "generateMonthlyAdvice: success, length=${advice.length}, cleaned=${cleaned.length}")
                _uiState.value = _uiState.value.copy(
                    adviceState = MonthlyAdviceState(
                        isLoading = false,
                        content = cleaned
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateMonthlyAdvice failed", e)
                _uiState.value = _uiState.value.copy(
                    adviceState = MonthlyAdviceState(
                        isLoading = false,
                        errorMessage = "建议生成失败，可以稍后再试。"
                    )
                )
            }
        }
    }

    fun clearAdviceError() {
        val current = _uiState.value.adviceState
        _uiState.value = _uiState.value.copy(
            adviceState = current.copy(errorMessage = null)
        )
    }

    private fun sanitizeMarkdown(text: String): String {
        return text
            .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")        // # headers
            .replace("**", "")                                               // **bold**
            .replace(Regex("```[\\s\\S]*?```"), "")                          // ```code blocks```
            .replace("`", "")                                                // inline `code`
            .replace(Regex("^>\\s*", RegexOption.MULTILINE), "")             // > blockquotes
            .replace(Regex("^[-\\*_]{3,}\\s*$", RegexOption.MULTILINE), "") // ---/***/___ HRs
            .replace(Regex("^[*\\-]\\s+", RegexOption.MULTILINE), "")       // - / * bullets
            .replace(Regex("^\\|.*\\|\\s*$", RegexOption.MULTILINE), "")    // | table rows |
            .replace(Regex("\\n{3,}"), "\n\n")                               // normalize excessive newlines
            .trim()
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared")
        super.onCleared()
        adviceRepository.cleanup()
        observeJob?.cancel()
    }
}
