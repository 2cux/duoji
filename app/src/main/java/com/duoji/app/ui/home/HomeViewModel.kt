package com.duoji.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.data.store.TransactionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val monthlyExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val balance: Double = 0.0,
    val topCategories: List<Pair<String, Double>> = emptyList(),
    val transactionCount: Int = 0,
    val aiTip: String = ""
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(
        aiTip = getDefaultTip(0.0)
    ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val year = now.year
            val month = now.monthValue

            val monthlyExpense = TransactionStore.getMonthlyExpenseTotal(year, month)
            val monthlyIncome = TransactionStore.getMonthlyIncomeTotal(year, month)
            val todayExpense = TransactionStore.getTodayExpenseTotal()
            val topCategories = TransactionStore.getTopCategories(year, month)
            val count = TransactionStore.transactions.size

            _uiState.value = HomeUiState(
                monthlyExpense = monthlyExpense,
                monthlyIncome = monthlyIncome,
                todayExpense = todayExpense,
                balance = monthlyIncome - monthlyExpense,
                topCategories = topCategories,
                transactionCount = count,
                aiTip = generateTip(monthlyExpense, monthlyIncome, count)
            )
        }
    }

    private fun generateTip(expense: Double, income: Double, count: Int): String {
        return when {
            count == 0 -> "用起来记录你的第一笔账吧 🌟"
            expense == 0.0 -> "这个月还没有支出记录哦"
            expense < 500 -> "今天消费节奏还不错，继续保持~"
            expense < 2000 -> "这个月花得有点快，留意一下哦"
            else -> "这个月已经支出 ${expense.toLong()} 元了，看看都花在哪了吧"
        }
    }

    companion object {
        fun getDefaultTip(expense: Double): String {
            return if (expense == 0.0) "用起来记录你的第一笔账吧 🌟"
            else "今天也要好好记账哦~"
        }
    }
}
