package com.duoji.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoji.app.DuoJiApplication
import com.duoji.app.data.ai.TrendTipRepository
import com.duoji.app.data.ai.TrendSummaryInput
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.data.repository.TransactionRepository
import com.duoji.app.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class TrendRange {
    LAST_7_DAYS, CURRENT_MONTH
}

data class DailyExpensePoint(
    val date: LocalDate,
    val amount: Double
)

data class ExpenseTrendSummary(
    val totalAmount: Double = 0.0,
    val averageDailyAmount: Double = 0.0,
    val maxDay: LocalDate? = null,
    val maxAmount: Double = 0.0,
    val recordDays: Int = 0
)

data class ExpenseTrendUiState(
    val range: TrendRange = TrendRange.LAST_7_DAYS,
    val points: List<DailyExpensePoint> = emptyList(),
    val summary: ExpenseTrendSummary = ExpenseTrendSummary(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val trendTip: String = "",
    val isTrendTipLoading: Boolean = false
)

data class HomeUiState(
    val monthlyExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val balance: Double = 0.0,
    val monthlyBudget: Double = -1.0,
    val remainingBudget: Double = 0.0,
    val averageDailyExpense: Double = 0.0,
    val expenseDaysCount: Int = 0,
    val topCategories: List<Pair<String, Double>> = emptyList(),
    val transactionCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val aiTip: String = ""
)

class HomeViewModel : ViewModel() {

    private val repository: TransactionRepository =
        DuoJiApplication.instance.container.transactionRepository

    private val settingsRepository: SettingsRepository =
        DuoJiApplication.instance.container.settingsRepository

    private val trendTipRepository = TrendTipRepository(
        DuoJiApplication.instance.container.settingsRepository
    )
    private var lastTrendTipCacheKey: String? = null
    private var trendTipJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState(aiTip = getDefaultTip(0.0)))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _trendState = MutableStateFlow(ExpenseTrendUiState())
    val trendState: StateFlow<ExpenseTrendUiState> = _trendState.asStateFlow()

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
        viewModelScope.launch {
            repository.observeAllTransactions().collect { transactions ->
                computeTrendData(transactions)
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val budget = settings.monthlyBudget
                val expense = _uiState.value.monthlyExpense
                val expenseDays = _uiState.value.expenseDaysCount
                val avgDaily = safeDiv(expense, expenseDays.toDouble())
                val remaining = if (budget > 0) safeAmount(budget - expense) else 0.0
                _uiState.value = _uiState.value.copy(
                    monthlyBudget = budget,
                    remainingBudget = remaining,
                    averageDailyExpense = avgDaily
                )
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

        val currentBudget = _uiState.value.monthlyBudget
        val expenseDaysCount = transactions
            .filter { it.type == "expense" }
            .map { TransactionRepository.millisToLocalDate(it.occurredAt) }
            .distinct()
            .count()
        val avgDaily = safeDiv(monthlyExpense, expenseDaysCount.toDouble())
        val remaining = if (currentBudget > 0) safeAmount(currentBudget - monthlyExpense) else 0.0

        _uiState.value = HomeUiState(
            monthlyExpense = monthlyExpense,
            monthlyIncome = monthlyIncome,
            todayExpense = todayExpense,
            balance = monthlyIncome - monthlyExpense,
            monthlyBudget = currentBudget,
            remainingBudget = remaining,
            averageDailyExpense = avgDaily,
            expenseDaysCount = expenseDaysCount,
            topCategories = expenseByCategory,
            transactionCount = count,
            recentTransactions = _uiState.value.recentTransactions,
            aiTip = generateTip(monthlyExpense, monthlyIncome, count, expenseByCategory)
        )
    }

    fun setTrendRange(range: TrendRange) {
        _trendState.value = _trendState.value.copy(range = range, isLoading = true)
        viewModelScope.launch {
            repository.observeAllTransactions().first().let { transactions ->
                computeTrendData(transactions)
            }
        }
    }

    private fun computeTrendData(transactions: List<TransactionEntity>) {
        val currentState = _trendState.value
        val range = currentState.range
        val now = LocalDate.now()

        val (startDate, endDate) = when (range) {
            TrendRange.LAST_7_DAYS -> now.minusDays(6) to now
            TrendRange.CURRENT_MONTH -> now.withDayOfMonth(1) to now
        }

        val startMs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val expenseTransactions = transactions
            .filter { it.type == "expense" && it.occurredAt in startMs until endMs }

        val expensesByDate = expenseTransactions
            .groupBy { TransactionRepository.millisToLocalDate(it.occurredAt) }
            .mapValues { safeAmount(it.value.sumOf { t -> t.amount }) }

        val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt()
        val points = (0..daysBetween).map { days ->
            val date = startDate.plusDays(days.toLong())
            DailyExpensePoint(
                date = date,
                amount = expensesByDate[date] ?: 0.0
            )
        }

        val totalAmount = safeAmount(points.sumOf { it.amount })
        val recordDays = points.count { it.amount > 0 }
        val averageDailyAmount = safeDiv(totalAmount, recordDays.toDouble())
        val maxPoint = if (points.isEmpty()) null else points.maxByOrNull { safeAmount(it.amount) }
        val maxDay = maxPoint?.date
        val maxAmount = safeAmount(maxPoint?.amount ?: 0.0)

        // ── Compute trend tip input ──
        val hasExpenseData = expenseTransactions.isNotEmpty()
        val topCategoryName: String
        val topCategoryAmount: Double
        if (hasExpenseData) {
            val expensesByCategory = expenseTransactions
                .groupBy { it.category }
                .mapValues { safeAmount(it.value.sumOf { t -> t.amount }) }
            val top = expensesByCategory.maxByOrNull { it.value }
            topCategoryName = top?.key ?: ""
            topCategoryAmount = top?.value ?: 0.0
        } else {
            topCategoryName = ""
            topCategoryAmount = 0.0
        }

        val trendDirection = computeTrendDirection(points)

        val budget = _uiState.value.monthlyBudget
        val monthlyExpense = _uiState.value.monthlyExpense
        val budgetLeft = if (budget > 0 && range == TrendRange.CURRENT_MONTH) {
            safeAmount(budget - monthlyExpense)
        } else null

        val topDayStr = if (maxPoint != null && maxPoint.amount > 0) {
            "${maxPoint.date.monthValue}月${maxPoint.date.dayOfMonth}日"
        } else ""

        val trendInput = TrendSummaryInput(
            range = if (range == TrendRange.LAST_7_DAYS) "近7天" else "本月",
            totalExpense = totalAmount,
            averageExpense = averageDailyAmount,
            topDay = topDayStr,
            topCategoryName = topCategoryName,
            topCategoryAmount = topCategoryAmount,
            trendDirection = trendDirection,
            budgetLeft = budgetLeft,
            hasData = hasExpenseData
        )

        val cacheKey = if (hasExpenseData) trendInput.cacheKey() else null
        val dataChanged = hasExpenseData && cacheKey != lastTrendTipCacheKey
        if (dataChanged) lastTrendTipCacheKey = cacheKey

        val tipToShow = when {
            !hasExpenseData -> "还没有足够数据，先轻松记一笔吧。"
            dataChanged -> trendTipRepository.generateLocalTip(trendInput)
            else -> _trendState.value.trendTip
        }

        _trendState.value = currentState.copy(
            points = points,
            summary = ExpenseTrendSummary(
                totalAmount = totalAmount,
                averageDailyAmount = averageDailyAmount,
                maxDay = maxDay,
                maxAmount = maxAmount,
                recordDays = recordDays
            ),
            isLoading = false,
            errorMessage = null,
            trendTip = tipToShow,
            isTrendTipLoading = dataChanged
        )

        // Trigger AI in background if data changed
        if (dataChanged) {
            trendTipJob?.cancel()
            trendTipJob = viewModelScope.launch {
                val aiTip = trendTipRepository.generateTip(trendInput)
                _trendState.value = _trendState.value.copy(trendTip = aiTip, isTrendTipLoading = false)
            }
        }
    }

    private fun safeAmount(value: Double): Double {
        return if (value.isNaN() || value.isInfinite()) 0.0 else value
    }

    private fun safeDiv(a: Double, b: Double): Double {
        return if (b == 0.0 || a.isNaN() || a.isInfinite()) 0.0 else a / b
    }

    private fun generateTip(expense: Double, income: Double, count: Int, topCategories: List<Pair<String, Double>>): String {
        if (count == 0) return "今天也可以轻松记一笔。"
        if (expense == 0.0) return "这个月还没有支出记录哦"
        val topCat = topCategories.firstOrNull()?.first
        if (topCat == "餐饮") return "这个月餐饮有点活跃，可以留意一下外卖和咖啡。"
        if (topCat == "购物") return "这个月购物记录比较多，可以看看哪些是真正需要的。"
        if (expense < 500) return "这个月消费节奏还不错。"
        return "这个月已经支出 ${expense.toLong()} 元了，可以看看都花在哪了。"
    }

    private fun computeTrendDirection(points: List<DailyExpensePoint>): String {
        val amounts = points.map { it.amount }
        if (amounts.size < 3) return "平稳"
        val recent3 = amounts.takeLast(3)
        val recentAvg = recent3.sum() / 3.0
        val overallAvg = amounts.sum() / amounts.size
        return if (recentAvg > overallAvg * 1.15) "上升"
        else if (recentAvg < overallAvg * 0.85) "下降"
        else "平稳"
    }

    override fun onCleared() {
        super.onCleared()
        trendTipJob?.cancel()
        trendTipRepository.cleanup()
    }

    companion object {
        fun getDefaultTip(expense: Double): String {
            return if (expense == 0.0) "今天也可以轻松记一笔。"
            else "今天也要好好记账哦~"
        }
    }
}
