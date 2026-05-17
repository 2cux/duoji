package com.duoji.app.data.export

import com.duoji.app.data.local.entity.TransactionEntity
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object JsonExportBuilder {

    fun build(transactions: List<TransactionEntity>): String {
        val now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"app\": \"duoji\",")
        sb.appendLine("  \"version\": \"1.0.0\",")
        sb.appendLine("  \"exported_at\": \"$now\",")
        sb.appendLine("  \"transaction_count\": ${transactions.size},")
        sb.appendLine("  \"transactions\": [")

        transactions.forEachIndexed { index, tx ->
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escapeJson(tx.id)}\",")
            sb.appendLine("      \"type\": \"${escapeJson(tx.type)}\",")
            sb.appendLine("      \"amount\": ${tx.amount},")
            sb.appendLine("      \"currency\": \"${escapeJson(tx.currency)}\",")
            sb.appendLine("      \"category\": \"${escapeJson(tx.category)}\",")
            sb.appendLine("      \"subcategory\": ${jsonNullableString(tx.subcategory)},")
            sb.appendLine("      \"note\": \"${escapeJson(tx.note)}\",")
            sb.appendLine("      \"merchantOrItem\": ${jsonNullableString(tx.merchantOrItem)},")
            sb.appendLine("      \"occurredAt\": ${tx.occurredAt},")
            sb.appendLine("      \"source\": \"${escapeJson(tx.source)}\",")
            sb.appendLine("      \"confidence\": ${tx.confidence},")
            sb.appendLine("      \"needUserConfirm\": ${tx.needUserConfirm},")
            sb.appendLine("      \"createdAt\": ${tx.createdAt},")
            sb.appendLine("      \"updatedAt\": ${tx.updatedAt}")
            if (index < transactions.lastIndex) {
                sb.appendLine("    },")
            } else {
                sb.appendLine("    }")
            }
        }

        sb.appendLine("  ]")
        sb.append("}")
        return sb.toString()
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun jsonNullableString(value: String?): String {
        return if (value.isNullOrBlank()) "null" else "\"${escapeJson(value)}\""
    }
}
