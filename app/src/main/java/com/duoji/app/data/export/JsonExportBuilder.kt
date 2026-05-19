package com.duoji.app.data.export

import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.data.util.DateUtils

object JsonExportBuilder {

    fun build(transactions: List<TransactionEntity>, appVersion: String = "1.0.0"): String {
        val sb = StringBuilder()
        val now = DateUtils.formatDateTime(System.currentTimeMillis())

        sb.appendLine("{")
        sb.appendLine("  \"exportedAt\": \"$now\",")
        sb.appendLine("  \"app\": \"duoji\",")
        sb.appendLine("  \"version\": \"${escapeJson(appVersion)}\",")
        sb.appendLine("  \"transactionCount\": ${transactions.size},")
        sb.appendLine("  \"transactions\": [")

        transactions.forEachIndexed { index, tx ->
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escapeJson(tx.id)}\",")
            sb.appendLine("      \"type\": \"${escapeJson(tx.type)}\",")
            sb.appendLine("      \"amount\": ${tx.amount},")
            sb.appendLine("      \"currency\": \"${escapeJson(tx.currency)}\",")
            sb.appendLine("      \"category\": \"${escapeJson(tx.category)}\",")
            sb.appendLine("      \"subcategory\": ${jsonValue(tx.subcategory)},")
            sb.appendLine("      \"note\": \"${escapeJson(tx.note)}\",")
            sb.appendLine("      \"merchantOrItem\": ${jsonValue(tx.merchantOrItem)},")
            sb.appendLine("      \"occurredAt\": \"${DateUtils.formatDateTime(tx.occurredAt)}\",")
            sb.appendLine("      \"occurredAtText\": ${jsonValue(tx.occurredAtText)},")
            sb.appendLine("      \"source\": \"${escapeJson(tx.source)}\",")
            sb.appendLine("      \"rawText\": ${jsonValue(tx.rawText)},")
            sb.appendLine("      \"confidence\": ${tx.confidence},")
            sb.appendLine("      \"needUserConfirm\": ${tx.needUserConfirm},")
            sb.appendLine("      \"createdAt\": \"${DateUtils.formatDateTime(tx.createdAt)}\",")
            sb.appendLine("      \"updatedAt\": \"${DateUtils.formatDateTime(tx.updatedAt)}\"")
            sb.appendLine(if (index < transactions.lastIndex) "    }," else "    }")
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

    private fun jsonValue(value: String?): String {
        return if (value.isNullOrBlank()) "null" else "\"${escapeJson(value)}\""
    }
}
