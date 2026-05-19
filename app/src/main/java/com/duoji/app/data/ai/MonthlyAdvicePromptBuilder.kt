package com.duoji.app.data.ai

import com.duoji.app.domain.statistics.MonthlyStatistics

object MonthlyAdvicePromptBuilder {

    fun buildPrompt(statistics: MonthlyStatistics): String {
        val summaryJson = buildSummaryJson(statistics)
        return """
你是一个温和的财务助手，请根据用户的月度消费统计摘要生成消费建议。

重要：不要使用任何 Markdown 格式符号。
- 不要使用 #、##、### 等标题符号
- 不要使用 **加粗**
- 不要使用 ``` 代码块
- 不要使用表格
- 不要使用 > 引用
- 不要使用 --- 分隔线
- 不要使用 - 或 * 作为列表标记

输出结构：
第一段用一句话概括本月财务概况（支出、收入、结余）。然后用 2-4 条具体建议，每条单独成行。每条建议必须结合具体金额、分类或消费频率来写。语气温和，不制造焦虑。

要求：
1. 基于统计摘要生成建议，不要编造数据。
2. 必须提到具体金额、类别。
3. 不要说"严重超支""消费异常""预算不足""必须减少"等强刺激表达。
4. 输出纯中文普通段落，不要输出任何格式符号。
5. 不要输出空泛内容，不要出现"理性消费"这类空话。

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
