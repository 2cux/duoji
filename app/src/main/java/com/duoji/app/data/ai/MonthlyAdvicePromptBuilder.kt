package com.duoji.app.data.ai

import com.duoji.app.domain.statistics.MonthlyStatistics

object MonthlyAdvicePromptBuilder {

    fun buildPrompt(statistics: MonthlyStatistics): String {
        val summaryJson = buildSummaryJson(statistics)
        return """
你是一个温和的财务助手，请根据用户的月度消费统计摘要生成一段消费建议（150-300字）。

要求：
1. 基于统计摘要生成建议，不要编造数据。
2. 不要输出空泛建议，必须提到具体金额、类别或行为。
3. 文案温和，不制造财务焦虑。
4. 不要说"你严重超支""消费异常""预算不足""必须减少"等强刺激表达。
5. 建议包含：
   - 本月财务概况
   - 支出最多的类别
   - 高频小额消费（如果有）
   - 可优化的一到三个具体行动
   - 下月建议
6. 输出中文自然语言即可，不需要 JSON 格式。

以下是本月统计摘要 JSON：
$summaryJson
""".trimIndent()
    }

    private fun buildSummaryJson(statistics: MonthlyStatistics): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"month\": \"${statistics.year}-${String.format("%02d", statistics.month)}\",")
        sb.appendLine("  \"income\": ${formatNum(statistics.totalIncome)},")
        sb.appendLine("  \"expense\": ${formatNum(statistics.totalExpense)},")
        sb.appendLine("  \"balance\": ${formatNum(statistics.balance)},")
        sb.appendLine("  \"transaction_count\": ${statistics.transactionCount},")

        // Category summary
        sb.appendLine("  \"category_summary\": [")
        statistics.categorySummaries.forEachIndexed { index, cat ->
            val comma = if (index < statistics.categorySummaries.lastIndex) "," else ""
            sb.appendLine("    {\"category\": \"${cat.category}\", \"amount\": ${formatNum(cat.amount)}, \"percentage\": ${formatNum(cat.percentage)}, \"count\": ${cat.count}}$comma")
        }
        sb.appendLine("  ],")

        // Top expense
        statistics.topExpense?.let { tx ->
            sb.appendLine("  \"top_expense\": {")
            sb.appendLine("    \"amount\": ${formatNum(tx.amount)},")
            sb.appendLine("    \"category\": \"${tx.category}\",")
            sb.appendLine("    \"note\": \"${tx.note}\"")
            sb.appendLine("  },")
        } ?: run {
            sb.appendLine("  \"top_expense\": null,")
        }

        // Frequent small expenses
        sb.appendLine("  \"frequent_small_expenses\": [")
        statistics.frequentSmallExpenses.forEachIndexed { index, fse ->
            val comma = if (index < statistics.frequentSmallExpenses.lastIndex) "," else ""
            sb.appendLine("    {\"name\": \"${fse.name}\", \"count\": ${fse.count}, \"amount\": ${formatNum(fse.amount)}}$comma")
        }
        sb.appendLine("  ]")
        sb.appendLine("}")

        return sb.toString()
    }

    private fun formatNum(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
    }
}
