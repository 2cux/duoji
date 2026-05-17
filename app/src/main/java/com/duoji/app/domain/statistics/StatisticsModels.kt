package com.duoji.app.domain.statistics

import com.duoji.app.data.local.entity.TransactionEntity

data class MonthlyStatistics(
    val year: Int,
    val month: Int,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val categorySummaries: List<CategorySummary> = emptyList(),
    val dailySummaries: List<DailySummary> = emptyList(),
    val topCategories: List<CategorySummary> = emptyList(),
    val topExpense: TransactionEntity? = null,
    val frequentSmallExpenses: List<FrequentSmallExpense> = emptyList(),
    val transactionCount: Int = 0,
    val expenseCount: Int = 0,
    val incomeCount: Int = 0
)

data class CategorySummary(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val count: Int
)

data class DailySummary(
    val date: String,
    val timestamp: Long,
    val expense: Double,
    val income: Double,
    val count: Int
)

data class FrequentSmallExpense(
    val name: String,
    val category: String,
    val count: Int,
    val amount: Double
)

data class MonthlyAdviceState(
    val isLoading: Boolean = false,
    val content: String? = null,
    val errorMessage: String? = null
)
