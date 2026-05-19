package com.duoji.app.data.ai

import com.duoji.app.data.model.AITransaction
import com.duoji.app.data.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Local mock parser that simulates AI parsing using regex patterns.
 * Used for demo/testing when no real AI API is configured.
 *
 * Supports time words (今天/昨天/前天), category and time inheritance,
 * connector splitting (加上/和/还有), and time-of-day extraction.
 */
object LocalMockParser {

    // Category keyword mapping (longer keywords first for priority matching)
    private val categoryMap = mapOf(
        // 餐饮
        "出租车" to Pair("交通", "打车"),
        "买菜" to Pair("餐饮", "买菜"),
        "凉菜" to Pair("餐饮", "正餐"),
        "午饭" to Pair("餐饮", "正餐"),
        "午餐" to Pair("餐饮", "正餐"),
        "晚饭" to Pair("餐饮", "正餐"),
        "晚餐" to Pair("餐饮", "正餐"),
        "吃饭" to Pair("餐饮", "正餐"),
        "外卖" to Pair("餐饮", "正餐"),
        "面包" to Pair("餐饮", "正餐"),
        "火锅" to Pair("餐饮", "正餐"),
        "早餐" to Pair("餐饮", "早餐"),
        "早饭" to Pair("餐饮", "早餐"),
        "咖啡" to Pair("餐饮", "饮品"),
        "奶茶" to Pair("餐饮", "饮品"),
        "饮料" to Pair("餐饮", "饮品"),
        "啤酒" to Pair("餐饮", "酒水"),
        "水果" to Pair("餐饮", "水果"),
        "蔬菜" to Pair("餐饮", "买菜"),
        "生鲜" to Pair("餐饮", "买菜"),
        "零食" to Pair("餐饮", "零食"),
        "食材" to Pair("餐饮", "买菜"),
        "水" to Pair("餐饮", "饮品"),
        "菜" to Pair("餐饮", "买菜"),
        // 交通
        "地铁" to Pair("交通", "地铁"),
        "公交" to Pair("交通", "公交"),
        "打车" to Pair("交通", "打车"),
        "高铁" to Pair("交通", "交通"),
        "加油" to Pair("交通", "加油"),
        "停车" to Pair("交通", "停车"),
        "过路" to Pair("交通", "过路费"),
        "单车" to Pair("交通", "单车"),
        // 购物
        "面膜" to Pair("购物", "美妆"),
        "护肤" to Pair("购物", "美妆"),
        "淘宝" to Pair("购物", "日用"),
        "京东" to Pair("购物", "日用"),
        "日用品" to Pair("购物", "日用"),
        "购物" to Pair("购物", "日用"),
        "衣服" to Pair("购物", "服饰"),
        // 居住
        "房租" to Pair("居住", "房租"),
        "水电" to Pair("居住", "水电"),
        "物业" to Pair("居住", "物业"),
        // 娱乐
        "电影" to Pair("娱乐", "电影"),
        "游戏" to Pair("娱乐", "游戏"),
        "健身" to Pair("娱乐", "健身"),
        // 通讯
        "话费" to Pair("通讯", "话费"),
        "流量" to Pair("通讯", "流量"),
        "网费" to Pair("通讯", "网费"),
        // 收入
        "工资" to Pair("工资", ""),
        "奖金" to Pair("工资", ""),
        "绩效" to Pair("工资", ""),
        "副业" to Pair("副业", ""),
        "红包" to Pair("红包", ""),
        // 退款 & 还款
        "退款" to Pair("退款", ""),
        // 学习
        "书本" to Pair("学习", "书籍"),
        "书籍" to Pair("学习", "书籍"),
        "课程" to Pair("学习", "课程"),
        "考试" to Pair("学习", "考试"),
        // 医疗
        "医疗" to Pair("医疗", ""),
        "挂号" to Pair("医疗", "挂号"),
        "买药" to Pair("医疗", "买药"),
        // 人情
        "人情" to Pair("人情", ""),
        "礼物" to Pair("人情", "礼物"),
        "聚餐" to Pair("人情", "聚餐"),
        // 旅行
        "旅行" to Pair("旅行", ""),
        "酒店" to Pair("旅行", "酒店"),
    )

    // Time word mapping: word -> day offset from today
    private val timeDayOffsetMap = mapOf(
        "今天" to 0,
        "今天早上" to 0,
        "今天上午" to 0,
        "今天中午" to 0,
        "今天下午" to 0,
        "今天晚上" to 0,
        "昨天" to -1,
        "昨天早上" to -1,
        "昨天上午" to -1,
        "昨天中午" to -1,
        "昨天下午" to -1,
        "昨天晚上" to -1,
        "前天" to -2,
        "前天早上" to -2,
        "前天上午" to -2,
        "前天内中午" to -2,
        "前天下午" to -2,
        "前天天晚上" to -2,
        "早上" to null,
        "上午" to null,
        "中午" to null,
        "下午" to null,
        "晚上" to null,
    )

    // Time-of-day mapping: word -> time string (HH:mm)
    private val timeOfDayMap = mapOf(
        "凌晨" to "06:00",
        "早上" to "08:00",
        "上午" to "10:00",
        "中午" to "12:00",
        "下午" to "14:00",
        "晚上" to "18:00",
        "半夜" to "23:00",
    )

    private val incomeKeywords = listOf("工资", "副业", "收入")
    private val refundKeywords = listOf("退款", "退钱", "退货", "返还")
    private val repaymentKeywords = listOf("还款", "花呗", "信用卡", "借呗")

    /**
     * Split input into sub-segments, each containing at most one amount.
     * Handles connectors like "加上", "和", "还有", "再加", "另外".
     */
    private fun splitToSubSegments(input: String): List<String> {
        // 1. Split by main separators (comma, semicolon, Chinese comma, etc.)
        val mainParts = input.split(Regex("[，,、；;。]+")).filter { it.isNotBlank() }
        // 2. Split each part by connectors
        val result = mutableListOf<String>()
        for (part in mainParts) {
            val subParts = part.split(Regex("(?:加上|还有|再加|另外|和|\\&|以及)")).filter { it.isNotBlank() }
            result.addAll(subParts)
        }
        return result
    }

    /**
     * Parse multi-segment input with category and time inheritance.
     */
    fun parseMulti(input: String): List<AITransaction> {
        if (input.isBlank()) return emptyList()

        val segments = splitToSubSegments(input)
        if (segments.isEmpty()) return emptyList()

        val results = mutableListOf<AITransaction>()

        // Context tracking across segments
        var contextCategory: Pair<String, String>? = null
        var contextDayOffset: Int? = null
        var contextTimeOfDay: String? = null

        for (segment in segments) {
            val parsed = parseSingle(segment, contextDayOffset, contextTimeOfDay)
            if (parsed == null) continue

            // Update context based on this segment
            val (extractedCategory, extractedDayOffset, extractedTimeOfDay) = parsed
            var transaction = extractedCategory

            // Category inheritance within parser
            val cat = extractedCategory.category
            val subcat = extractedCategory.subcategory ?: ""
            if (cat != "其他" && cat.isNotBlank()) {
                contextCategory = Pair(cat, subcat)
            } else if (contextCategory != null) {
                // Inherit category
                transaction = transaction.copy(
                    category = contextCategory!!.first,
                    subcategory = contextCategory!!.second.ifEmpty { null }
                )
            }

            // Time inheritance within parser
            val dayOffset = extractedDayOffset ?: contextDayOffset ?: 0
            val timeOfDay = extractedTimeOfDay ?: contextTimeOfDay ?: "12:00"
            if (extractedDayOffset != null) contextDayOffset = extractedDayOffset
            if (extractedTimeOfDay != null) contextTimeOfDay = extractedTimeOfDay

            val today = LocalDate.now()
            val date = today.plusDays(dayOffset.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            transaction = transaction.copy(occurredAt = "${dateStr}T${timeOfDay}:00+08:00")

            results.add(transaction)
        }

        return results
    }

    /**
     * Parse a single segment that should contain at most one amount.
     * Returns the AITransaction and extracted time context, or null if no amount found.
     */
    private data class ParsedSegment(
        val transaction: AITransaction,
        val dayOffset: Int?,
        val timeOfDay: String?
    )

    private fun parseSingle(
        text: String,
        defaultDayOffset: Int?,
        defaultTimeOfDay: String?
    ): ParsedSegment? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null

        // 1. Extract amount
        val amount = extractFirstAmount(trimmed)
        if (amount == null) return null

        val textWithoutAmount = removeFirstAmount(trimmed)
            .removeSuffix("块").removeSuffix("元").removeSuffix("毛")
            .trim()

        // 2. Extract time context
        val (dayOffset, timeOfDay, textAfterTime) = extractTimeContext(textWithoutAmount)

        // 3. Detect type and category
        val cleanedText = textAfterTime.trim()
        val type = detectType(cleanedText)
        var category = detectCategory(cleanedText)

        // 4. Build transaction
        val isIncome = type == TransactionType.INCOME
        val isRefund = type == TransactionType.REFUND
        val isRepayment = type == TransactionType.REPAYMENT
        val confidence = if (category.first != "其他") 0.85 else 0.55
        val needUserConfirm = confidence < 0.7

        val typeStr = when (type) {
            TransactionType.INCOME -> "income"
            TransactionType.REFUND -> "refund"
            TransactionType.REPAYMENT -> "repayment"
            TransactionType.TRANSFER -> "transfer"
            else -> "expense"
        }

        val note = cleanedText.ifBlank { null }

        return ParsedSegment(
            transaction = AITransaction(
                type = typeStr,
                amount = amount,
                category = category.first,
                subcategory = category.second.ifEmpty { null },
                timeText = "",
                occurredAt = "", // filled by caller
                merchantOrItem = note,
                note = note,
                confidence = confidence,
                needUserConfirm = needUserConfirm
            ),
            dayOffset = dayOffset,
            timeOfDay = timeOfDay
        )
    }

    /**
     * Extract time context (day offset and time-of-day) from the beginning of text.
     */
    private fun extractTimeContext(text: String): Triple<Int?, String?, String> {
        var remaining = text
        var dayOffset: Int? = null
        var timeOfDay: String? = null

        // Try to match time words (longest first to avoid partial matches)
        val sortedTimeWords = timeDayOffsetMap.entries
            .filter { it.key.length >= 2 }
            .sortedByDescending { it.key.length }

        for ((word, offset) in sortedTimeWords) {
            if (remaining.startsWith(word)) {
                if (offset != null) dayOffset = offset
                remaining = remaining.removePrefix(word).trim()
                // Extract time-of-day from the matched word
                for ((todWord, todTime) in timeOfDayMap) {
                    if (word.contains(todWord)) {
                        timeOfDay = todTime
                        break
                    }
                }
                break
            }
        }

        // Also check standalone time-of-day words in remaining text
        if (timeOfDay == null) {
            for ((word, time) in timeOfDayMap) {
                if (remaining.startsWith(word)) {
                    timeOfDay = time
                    remaining = remaining.removePrefix(word).trim()
                    break
                }
            }
        }

        return Triple(dayOffset, timeOfDay, remaining)
    }

    /**
     * Extract the first numeric amount from text.
     */
    private fun extractFirstAmount(text: String): Double? {
        val regex = Regex("""(\d+(\.\d+)?)""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    /**
     * Remove the first numeric amount from text.
     */
    private fun removeFirstAmount(text: String): String {
        return text.replaceFirst(Regex("""\d+(\.\d+)?"""), "").trim()
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
