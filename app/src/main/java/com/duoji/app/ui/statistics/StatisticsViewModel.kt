package com.duoji.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.DuoJiApplication
import com.duoji.app.data.ai.MonthlyAdviceRepository
import com.duoji.app.data.repository.TransactionRepository
import com.duoji.app.domain.statistics.MonthlyAdviceState
import com.duoji.app.domain.statistics.MonthlyStatistics
import com.duoji.app.domain.statistics.StatisticsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

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

    private val adviceRepository = MonthlyAdviceRepository()

    private val useCase = StatisticsUseCase()

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadMonth(_uiState.value.selectedYear, _uiState.value.selectedMonth)
    }

    fun loadCurrentMonth() {
        val now = LocalDate.now()
        loadMonth(now.year, now.monthValue)
    }

    fun previousMonth() {
        val s = _uiState.value
        val date = LocalDate.of(s.selectedYear, s.selectedMonth, 1).minusMonths(1)
        loadMonth(date.year, date.monthValue)
    }

    fun nextMonth() {
        val s = _uiState.value
        val date = LocalDate.of(s.selectedYear, s.selectedMonth, 1)
        val now = LocalDate.now()
        val next = date.plusMonths(1)
        if (next.year > now.year || (next.year == now.year && next.monthValue > now.monthValue)) {
            return
        }
        loadMonth(next.year, next.monthValue)
    }

    private fun loadMonth(year: Int, month: Int) {
        _uiState.value = _uiState.value.copy(
            selectedYear = year,
            selectedMonth = month,
            isLoading = true,
            errorMessage = null,
            adviceState = MonthlyAdviceState()
        )

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeTransactionsByMonth(year, month).collect { transactions ->
                val statistics = useCase.buildMonthlyStatistics(transactions, year, month)
                _uiState.value = _uiState.value.copy(
                    statistics = statistics,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    fun refresh() {
        loadMonth(_uiState.value.selectedYear, _uiState.value.selectedMonth)
    }

    fun generateMonthlyAdvice() {
        val stats = _uiState.value.statistics ?: return
        _uiState.value = _uiState.value.copy(
            adviceState = MonthlyAdviceState(isLoading = true)
        )

        viewModelScope.launch {
            try {
                val advice = adviceRepository.generateAdvice(stats)
                _uiState.value = _uiState.value.copy(
                    adviceState = MonthlyAdviceState(
                        isLoading = false,
                        content = advice
                    )
                )
            } catch (e: Exception) {
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

    override fun onCleared() {
        super.onCleared()
        adviceRepository.cleanup()
        observeJob?.cancel()
    }
}
