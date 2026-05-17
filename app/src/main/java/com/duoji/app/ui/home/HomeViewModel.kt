package com.duoji.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.DuoJiApplication
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val monthlyExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val balance: Double = 0.0,
    val topCategories: List<Pair<String, Double>> = emptyList(),
    val transactionCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val aiTip: String = ""
)

class HomeViewModel : ViewModel() {

    private val repository: TransactionRepository =
        DuoJiApplication.instance.container.transactionRepository

    private val _uiState = MutableStateFlow(HomeUiState(aiTip = getDefaultTip(0.0)))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCurrentMonthTransactions().collect { transactions ->
                updateFromTransactions(transactions)
            }
        }
        viewModelScope.launch {
            repository.observeRecentTransactions(5).collect { recent ->
                _uiState.value = _uiState.value.copy(recentTransactions = recent)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.observeCurrentMonthTransactions().first().let { transactions ->
                updateFromTransactions(transactions)
            }
        }
    }

    private fun updateFromTransactions(transactions: List<TransactionEntity>) {
        val now = LocalDate.now()
        val monthlyExpense = transactions
            .filter { it.type == "expense" }
            .sumOf { it.amount }
        val monthlyIncome = transactions
            .filter { it.type == "income" }
            .sumOf { it.amount }

        val todayStartMs = now.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val todayEndMs = now.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val todayExpense = transactions
            .filter { it.type == "expense" && it.occurredAt in todayStartMs until todayEndMs }
            .sumOf { it.amount }

        val expenseByCategory = transactions
            .filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }

        val count = transactions.size

        _uiState.value = HomeUiState(
            monthlyExpense = monthlyExpense,
            monthlyIncome = monthlyIncome,
            todayExpense = todayExpense,
            balance = monthlyIncome - monthlyExpense,
            topCategories = expenseByCategory,
            transactionCount = count,
            recentTransactions = _uiState.value.recentTransactions,
            aiTip = generateTip(monthlyExpense, monthlyIncome, count)
        )
    }

    private fun generateTip(expense: Double, income: Double, count: Int): String {
        return when {
            count == 0 -> "用起来记录你的第一笔账吧"
            expense == 0.0 -> "这个月还没有支出记录哦"
            expense < 500 -> "今天消费节奏还不错，继续保持~"
            expense < 2000 -> "这个月花得有点快，留意一下哦"
            else -> "这个月已经支出 ${expense.toLong()} 元了，看看都花在哪了吧"
        }
    }

    companion object {
        fun getDefaultTip(expense: Double): String {
            return if (expense == 0.0) "用起来记录你的第一笔账吧"
            else "今天也要好好记账哦~"
        }
    }
}
