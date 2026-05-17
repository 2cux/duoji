package com.duoji.app.data.ai

import com.duoji.app.domain.statistics.FrequentSmallExpense
import com.duoji.app.domain.statistics.MonthlyStatistics

object LocalMonthlyAdviceGenerator {

    fun generate(statistics: MonthlyStatistics): String {
        if (statistics.transactionCount == 0) {
            return "这个月还没有足够的记录，先轻松记几笔，月底我再帮你看看钱主要花在哪里。"
        }

        if (statistics.totalExpense == 0.0 && statistics.totalIncome > 0) {
            return "这个月只记录了收入，还没有支出记录。继续保持就好，有消费时记得记上，这样月底分析会更完整。"
        }

        if (statistics.totalIncome == 0.0 && statistics.totalExpense > 0) {
            return "这个月目前只记录了支出，还没有收入记录。建议先把工资或主要收入也记上，这样结余会更准确。"
        }

        val sb = StringBuilder()
        val expenseStr = formatAmount(statistics.totalExpense)
        val incomeStr = formatAmount(statistics.totalIncome)
        val balanceStr = formatAmount(statistics.balance)

        sb.append("这个月你一共支出 ¥$expenseStr，收入 ¥$incomeStr，结余 ¥$balanceStr。")

        // Top category
        if (statistics.topCategories.isNotEmpty()) {
            val top = statistics.topCategories.first()
            val topStr = formatAmount(top.amount)
            val pct = String.format("%.0f", top.percentage)
            sb.append(" 支出最多的是「${top.category}」，共 ¥$topStr，占比 ${pct}%。")

            // More specific suggestion based on top category
            when (top.category) {
                "餐饮" -> sb.append(" 餐饮消费占比较高，可以留意一下外卖和外出就餐的频率。")
                "购物" -> sb.append(" 购物支出比较突出，可以看看哪些是真正需要的。")
                "交通" -> sb.append(" 交通费用占了一定比例，如果常打车可以考虑公共交通。")
                "娱乐" -> sb.append(" 娱乐开销不小，适当放松挺好的，也可以留意一下频率。")
            }
        }

        // Frequent small expenses
        if (statistics.frequentSmallExpenses.isNotEmpty()) {
            val topFreq = statistics.frequentSmallExpenses.first()
            val freqStr = formatAmount(topFreq.amount)
            sb.append(" 其中${topFreq.name}这类小额消费出现了 ${topFreq.count} 次，合计 ¥$freqStr。")

            if (statistics.frequentSmallExpenses.size > 1) {
                val second = statistics.frequentSmallExpenses[1]
                val secondStr = formatAmount(second.amount)
                sb.append(" ${second.name}也出现了 ${second.count} 次，合计 ¥$secondStr。")
            }
            sb.append(" 下个月可以试着把类似小额消费减少 2-3 次，会有明显变化。")
        }

        // Actionable advice
        if (statistics.topCategories.isNotEmpty()) {
            val top = statistics.topCategories.first()
            when {
                top.percentage > 40 -> {
                    sb.append(" 「${top.category}」占比超过四成，下个月可以有意识地关注这一块的支出。")
                }
                top.percentage > 30 -> {
                    sb.append(" 下个月可以先从留意「${top.category}」开始，不用太严格，心里有数就好。")
                }
                else -> {
                    sb.append(" 整体来看消费比较均衡，下个月继续保持这个节奏就好。")
                }
            }
        }

        // Closing
        if (statistics.balance > 0) {
            sb.append(" 这个月有结余，很棒。适度储蓄会让生活更有余地。")
        } else if (statistics.balance < 0 && statistics.totalExpense > 0) {
            sb.append(" 这个月支出略高于收入，不一定是问题，下个月稍微留意就好。")
        }

        sb.append(" 这些建议只是帮你看清消费节奏，不需要太有压力。")

        return sb.toString()
    }

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            String.format("%.1f", amount)
        }
    }
}
