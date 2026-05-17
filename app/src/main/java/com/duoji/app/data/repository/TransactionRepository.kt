package com.duoji.app.data.repository

import com.duoji.app.data.local.dao.TransactionDao
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.data.model.TransactionDraft
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TransactionRepository(private val dao: TransactionDao) {

    suspend fun saveDrafts(drafts: List<TransactionDraft>): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val entities = drafts.filter { it.amount != null }.map { draft ->
            TransactionEntity(
                id = draft.id.ifBlank { java.util.UUID.randomUUID().toString() },
                type = draft.type.name.lowercase(),
                amount = draft.amount!!,
                currency = draft.currency,
                category = draft.category,
                subcategory = draft.subcategory.ifBlank { null },
                note = draft.note,
                merchantOrItem = draft.merchantOrItem.ifBlank { null },
                occurredAt = parseIsoToMillis(draft.occurredAt),
                occurredAtText = draft.occurredAt.ifBlank { null },
                source = "ai_parse",
                rawText = null,
                confidence = draft.confidence,
                needUserConfirm = draft.needUserConfirm,
                createdAt = now,
                updatedAt = now
            )
        }
        if (entities.isNotEmpty()) {
            dao.insertTransactions(entities)
        }
        return entities
    }

    suspend fun saveTransaction(entity: TransactionEntity) {
        dao.insertTransaction(entity)
    }

    suspend fun updateTransaction(entity: TransactionEntity) {
        dao.updateTransaction(entity.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTransaction(entity: TransactionEntity) {
        dao.deleteTransaction(entity)
    }

    suspend fun deleteTransactionById(id: String) {
        dao.deleteTransactionById(id)
    }

    fun observeAllTransactions(): Flow<List<TransactionEntity>> =
        dao.observeAllTransactions()

    fun observeTransactionsByMonth(year: Int, month: Int): Flow<List<TransactionEntity>> {
        val start = LocalDate.of(year, month, 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = LocalDate.of(year, month, 1).plusMonths(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.observeTransactionsByMonth(start, end)
    }

    fun observeCurrentMonthTransactions(): Flow<List<TransactionEntity>> {
        val now = LocalDate.now()
        return observeTransactionsByMonth(now.year, now.monthValue)
    }

    fun observeTodayTransactions(): Flow<List<TransactionEntity>> {
        val today = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tomorrow = LocalDate.now().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.observeTransactionsByDateRange(today, tomorrow)
    }

    fun observeRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> =
        dao.observeRecentTransactions(limit)

    fun getTransactionById(id: String): Flow<TransactionEntity?> =
        dao.getTransactionById(id)

    suspend fun getAllTransactionsOnce(): List<TransactionEntity> =
        dao.getAllTransactionsOnce()

    suspend fun deleteAllTransactions() {
        dao.deleteAllTransactions()
    }

    companion object {
        fun parseIsoToMillis(iso: String): Long {
            if (iso.isBlank()) return System.currentTimeMillis()
            return try {
                val odt = OffsetDateTime.parse(iso, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                odt.toInstant().toEpochMilli()
            } catch (e: Exception) {
                try {
                    val ldt = LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (e2: Exception) {
                    try {
                        val d = LocalDate.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE)
                        d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } catch (e3: Exception) {
                        System.currentTimeMillis()
                    }
                }
            }
        }

        fun millisToLocalDate(millis: Long): LocalDate {
            return java.time.Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
        }

        fun millisToDateTimeString(millis: Long): String {
            val ldt = java.time.Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault()).toLocalDateTime()
            return ldt.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
        }

        fun millisToDateString(millis: Long): String {
            val d = millisToLocalDate(millis)
            return d.format(DateTimeFormatter.ofPattern("M月d日"))
        }
    }
}
