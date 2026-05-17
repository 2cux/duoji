package com.duoji.app.ui.bill

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.DuoJiApplication
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.data.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GroupedTransaction(
    val date: LocalDate,
    val transactions: List<TransactionEntity>,
    val totalExpense: Double
)

data class BillListUiState(
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val groupedTransactions: List<GroupedTransaction> = emptyList(),
    val monthExpense: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthBalance: Double = 0.0,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val refreshTrigger: Long = 0L
)

class BillListViewModel : ViewModel() {

    private val repository: TransactionRepository =
        DuoJiApplication.instance.container.transactionRepository

    private val _uiState = MutableStateFlow(BillListUiState())
    val uiState: StateFlow<BillListUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadMonth(LocalDate.now().year, LocalDate.now().monthValue)
    }

    fun loadMonth(year: Int, month: Int) {
        observeJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedYear = year,
            selectedMonth = month,
            isLoading = true
        )
        observeJob = viewModelScope.launch {
            repository.observeTransactionsByMonth(year, month).collect { transactions ->
                val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
                val income = transactions.filter { it.type == "income" }.sumOf { it.amount }

                val grouped = transactions
                    .groupBy {
                        TransactionRepository.millisToLocalDate(it.occurredAt)
                    }
                    .map { (date, list) ->
                        GroupedTransaction(
                            date = date,
                            transactions = list.sortedByDescending { it.occurredAt },
                            totalExpense = list
                                .filter { it.type == "expense" }
                                .sumOf { it.amount }
                        )
                    }
                    .sortedByDescending { it.date }

                _uiState.value = _uiState.value.copy(
                    groupedTransactions = grouped,
                    monthExpense = expense,
                    monthIncome = income,
                    monthBalance = income - expense,
                    isLoading = false,
                    isEmpty = transactions.isEmpty()
                )
            }
        }
    }

    fun previousMonth() {
        var y = _uiState.value.selectedYear
        var m = _uiState.value.selectedMonth - 1
        if (m < 1) { m = 12; y-- }
        loadMonth(y, m)
    }

    fun nextMonth() {
        var y = _uiState.value.selectedYear
        var m = _uiState.value.selectedMonth + 1
        if (m > 12) { m = 1; y++ }
        val now = LocalDate.now()
        if (y > now.year || (y == now.year && m > now.monthValue)) return
        loadMonth(y, m)
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    fun refresh() {
        loadMonth(_uiState.value.selectedYear, _uiState.value.selectedMonth)
    }
}
