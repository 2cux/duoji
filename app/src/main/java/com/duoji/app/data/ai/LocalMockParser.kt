package com.duoji.app.data.ai

import com.duoji.app.data.model.AITransaction
import com.duoji.app.data.model.TransactionType
import com.duoji.app.data.model.toTransactionDraft
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Local mock parser that simulates AI parsing using regex patterns.
 * Used for demo/testing when no real AI API is configured.
 */
object LocalMockParser {

    private val separators = Regex("[，,、;；\\s]+")

    // Category keyword mapping
    private val categoryMap = mapOf(
        "午饭" to Pair("餐饮", "正餐"),
        "午餐" to Pair("餐饮", "正餐"),
        "早饭" to Pair("餐饮", "早餐"),
        "早餐" to Pair("餐饮", "早餐"),
        "晚饭" to Pair("餐饮", "正餐"),
        "晚餐" to Pair("餐饮", "正餐"),
        "咖啡" to Pair("餐饮", "饮品"),
        "奶茶" to Pair("餐饮", "饮品"),
        "饮料" to Pair("餐饮", "饮品"),
        "水" to Pair("餐饮", "饮品"),
        "水果" to Pair("餐饮", "水果"),
        "零食" to Pair("餐饮", "零食"),
        "面包" to Pair("餐饮", "正餐"),
        "外卖" to Pair("餐饮", "正餐"),
        "地铁" to Pair("交通", "地铁"),
        "公交" to Pair("交通", "公交"),
        "打车" to Pair("交通", "打车"),
        "出租车" to Pair("交通", "打车"),
        "加油" to Pair("交通", "加油"),
        "停车" to Pair("交通", "停车"),
        "电影" to Pair("娱乐", "电影"),
        "游戏" to Pair("娱乐", "游戏"),
        "购物" to Pair("购物", "日用"),
        "衣服" to Pair("购物", "服饰"),
        "房租" to Pair("居住", "房租"),
        "水电" to Pair("居住", "水电"),
        "话费" to Pair("通讯", "话费"),
        "流量" to Pair("通讯", "流量"),
        "工资" to Pair("工资", ""),
        "红包" to Pair("红包", ""),
        "退款" to Pair("退款", ""),
    )

    private val incomeKeywords = listOf("工资", "副业", "收入")
    private val refundKeywords = listOf("退款", "退钱", "退货", "返还")
    private val repaymentKeywords = listOf("还款", "花呗", "信用卡")

    fun parse(input: String): AITransaction? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        val amount = extractAmount(trimmed)
        val cleanText = removeAmount(trimmed)

        val type = detectType(cleanText)
        val (category, subcategory) = detectCategory(cleanText)

        val merchantOrItem = if (cleanText.isNotBlank()) cleanText else null
        val confidence = if (category != "其他") 0.85 else 0.55
        val needUserConfirm = amount == null || confidence < 0.7

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        return AITransaction(
            type = when (type) {
                TransactionType.INCOME -> "income"
                TransactionType.REFUND -> "refund"
                TransactionType.REPAYMENT -> "repayment"
                TransactionType.TRANSFER -> "transfer"
                else -> "expense"
            },
            amount = amount,
            category = category,
            subcategory = subcategory.ifEmpty { null },
            timeText = "今天",
            occurredAt = "${today}T12:00:00+08:00",
            merchantOrItem = merchantOrItem,
            note = merchantOrItem,
            confidence = confidence,
            needUserConfirm = needUserConfirm
        )
    }

    fun parseMulti(input: String): List<AITransaction> {
        if (input.isBlank()) return emptyList()

        val segments = input.split(separators).filter { it.isNotBlank() }
        if (segments.isEmpty()) return emptyList()

        val results = segments.mapNotNull { parse(it) }

        // If only one segment but it has multiple amounts like "工资8000"
        if (results.isEmpty() && segments.size == 1) {
            val single = parse(input)
            if (single != null) return listOf(single)
        }

        return results
    }

    private fun extractAmount(text: String): Double? {
        val regex = Regex("""(\d+(\.\d+)?)""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun removeAmount(text: String): String {
        return text.replace(Regex("""\d+(\.\d+)?"""), "").trim()
            .removeSuffix("块").removeSuffix("元").removeSuffix("毛")
            .trim()
    }

    private fun detectType(text: String): TransactionType {
        val lowered = text.lowercase()
        if (repaymentKeywords.any { lowered.contains(it) }) return TransactionType.REPAYMENT
        if (refundKeywords.any { lowered.contains(it) }) return TransactionType.REFUND
        if (incomeKeywords.any { lowered.contains(it) }) return TransactionType.INCOME
        return TransactionType.EXPENSE
    }

    private fun detectCategory(text: String): Pair<String, String> {
        for ((keyword, pair) in categoryMap) {
            if (text.contains(keyword)) return pair
        }
        return Pair("其他", "")
    }
}
