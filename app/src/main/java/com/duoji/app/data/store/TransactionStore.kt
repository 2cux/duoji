package com.duoji.app.data.store

import androidx.compose.runtime.mutableStateListOf
import com.duoji.app.data.model.Transaction
import com.duoji.app.data.model.TransactionDraft
import com.duoji.app.data.model.TransactionType

/**
 * In-memory transaction store for Phase 1 (no Room yet).
 */
object TransactionStore {
    val transactions = mutableStateListOf<Transaction>()

    fun save(draft: TransactionDraft): Transaction {
        val transaction = Transaction(
            id = draft.id,
            type = draft.type,
            amount = draft.amount ?: 0.0,
            currency = draft.currency,
            category = draft.category,
            subcategory = draft.subcategory,
            note = draft.note,
            merchantOrItem = draft.merchantOrItem,
            occurredAt = draft.occurredAt
        )
        transactions.add(transaction)
        return transaction
    }

    fun delete(id: String) {
        transactions.removeAll { it.id == id }
    }

    fun update(transaction: Transaction) {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index >= 0) {
            transactions[index] = transaction.copy(updatedAt = System.currentTimeMillis())
        }
    }

    fun getMonthlyExpenses(year: Int, month: Int): List<Transaction> {
        return transactions.filter { t ->
            t.type == TransactionType.EXPENSE && t.occurredAt.startsWith("$year-${month.toTwoDigits()}")
        }
    }

    fun getMonthlyIncome(year: Int, month: Int): List<Transaction> {
        return transactions.filter { t ->
            t.type == TransactionType.INCOME && t.occurredAt.startsWith("$year-${month.toTwoDigits()}")
        }
    }

    fun getTodayExpenses(): List<Transaction> {
        val today = java.time.LocalDate.now().toString()
        return transactions.filter { t ->
            t.type == TransactionType.EXPENSE && t.occurredAt.startsWith(today)
        }
    }

    fun getMonthlyExpenseTotal(year: Int, month: Int): Double {
        return getMonthlyExpenses(year, month).sumOf { it.amount }
    }

    fun getMonthlyIncomeTotal(year: Int, month: Int): Double {
        return getMonthlyIncome(year, month).sumOf { it.amount }
    }

    fun getTodayExpenseTotal(): Double {
        return getTodayExpenses().sumOf { it.amount }
    }

    fun getTopCategories(year: Int, month: Int, limit: Int = 5): List<Pair<String, Double>> {
        val expenses = getMonthlyExpenses(year, month)
        return expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    fun getAll(): List<Transaction> = transactions.toList()

    private fun Int.toTwoDigits(): String = if (this < 10) "0$this" else "$this"
}

/**
 * Temporary holder for AI parse results passed between screens.
 */
object ParseResultStore {
    var drafts: List<TransactionDraft> = emptyList()
    /** Whether the parse result came from local fallback (AI was configured but failed) */
    var usingLocalFallback: Boolean = false
}
