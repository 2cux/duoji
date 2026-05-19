package com.duoji.app.domain.statistics

import android.util.Log
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.data.repository.TransactionRepository
import java.time.ZoneId

private const val TAG = "StatisticsUseCase"

class StatisticsUseCase {

    fun buildMonthlyStatistics(
        transactions: List<TransactionEntity>,
        year: Int,
        month: Int
    ): MonthlyStatistics {
        Log.d(TAG, "buildMonthlyStatistics: year=$year month=$month, ${transactions.size} transactions")

        val expenses = transactions.filter { it.type == "expense" }
        val incomes = transactions.filter { it.type == "income" }

        val totalExpense = expenses.sumOf { it.amount }
        val totalIncome = incomes.sumOf { it.amount }
        val balance = totalIncome - totalExpense
        val expenseCount = expenses.size
        val incomeCount = incomes.size

        Log.d(TAG, "expenses=$expenseCount (total=$totalExpense), incomes=$incomeCount (total=$totalIncome)")

        // Category summaries (expense only) — guard against null/empty categories
        val categorySummaries = expenses
            .filter { it.category.isNotBlank() }
            .groupBy { it.category }
            .map { (category, list) ->
                val amount = list.sumOf { it.amount }
                CategorySummary(
                    category = category,
                    amount = amount,
                    percentage = if (totalExpense > 0) amount / totalExpense * 100 else 0.0,
                    count = list.size
                )
            }
            .sortedByDescending { it.amount }

        // Top 3 categories
        val topCategories = categorySummaries.take(3)

        // Daily summaries — guard against invalid timestamps
        val dailySummaries = try {
            transactions
                .filter { tx -> tx.occurredAt > 0 }
                .groupBy { tx ->
                    TransactionRepository.millisToLocalDate(tx.occurredAt)
                }
                .map { (date, list) ->
                    DailySummary(
                        date = date.format(java.time.format.DateTimeFormatter.ofPattern("M/d")),
                        timestamp = date.atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli(),
                        expense = list.filter { it.type == "expense" }.sumOf { it.amount },
                        income = list.filter { it.type == "income" }.sumOf { it.amount },
                        count = list.size
                    )
                }
                .sortedBy { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "buildDailySummaries failed", e)
            emptyList()
        }

        // Top expense
        val topExpense = expenses.maxByOrNull { it.amount }

        // Frequent small expenses (<= 50)
        val frequentSmallExpenses = try {
            expenses
                .filter { it.amount > 0 && it.amount <= 50 }
                .groupBy { tx ->
                    (tx.merchantOrItem ?: tx.note).ifBlank { "其他" }
                }
                .map { (name, list) ->
                    FrequentSmallExpense(
                        name = name,
                        category = list.first().category,
                        count = list.size,
                        amount = list.sumOf { it.amount }
                    )
                }
                .filter { it.count >= 3 }
                .sortedByDescending { it.count }
                .take(5)
        } catch (e: Exception) {
            Log.e(TAG, "buildFrequentSmallExpenses failed", e)
            emptyList()
        }

        val result = MonthlyStatistics(
            year = year,
            month = month,
            totalIncome = totalIncome,
            totalExpense = if (totalExpense.isNaN() || totalExpense.isInfinite()) 0.0 else totalExpense,
            balance = if (balance.isNaN() || balance.isInfinite()) 0.0 else balance,
            categorySummaries = categorySummaries,
            dailySummaries = dailySummaries,
            topCategories = topCategories,
            topExpense = topExpense,
            frequentSmallExpenses = frequentSmallExpenses,
            transactionCount = transactions.size,
            expenseCount = expenseCount,
            incomeCount = incomeCount
        )

        Log.d(TAG, "buildMonthlyStatistics completed: categories=${categorySummaries.size}, " +
                "dailyDays=${dailySummaries.size}, topExpense=${topExpense?.amount ?: "none"}")
        return result
    }
}
