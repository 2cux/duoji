package com.duoji.app.data.export

import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.data.util.DateUtils

object CsvExportBuilder {

    private val HEADER = listOf(
        "id", "type", "amount", "currency", "category", "subcategory",
        "note", "merchantOrItem", "occurredAt", "source", "confidence",
        "needUserConfirm", "createdAt", "updatedAt"
    )

    fun build(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.appendLine(HEADER.joinToString(",") { escapeCsv(it) })
        transactions.forEach { tx ->
            sb.appendLine(
                listOf(
                    tx.id,
                    tx.type,
                    tx.amount.toString(),
                    tx.currency,
                    tx.category,
                    tx.subcategory ?: "",
                    tx.note,
                    tx.merchantOrItem ?: "",
                    DateUtils.formatDateTime(tx.occurredAt),
                    tx.source,
                    tx.confidence.toString(),
                    tx.needUserConfirm.toString(),
                    DateUtils.formatDateTime(tx.createdAt),
                    DateUtils.formatDateTime(tx.updatedAt)
                ).joinToString(",") { escapeCsv(it) }
            )
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
