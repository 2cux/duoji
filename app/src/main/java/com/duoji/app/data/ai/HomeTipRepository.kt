package com.duoji.app.data.ai

import android.util.Log
import com.duoji.app.data.model.*
import com.duoji.app.data.settings.SettingsRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

data class SmallExpenseSummary(
    val name: String,
    val count: Int,
    val amount: Double
)

data class HomeTipInput(
    val monthExpense: Double,
    val todayExpense: Double,
    val dailyAverage: Double,
    val topCategories: List<Pair<String, Double>>,
    val frequentSmallExpenses: List<SmallExpenseSummary>,
    val largestExpenseName: String,
    val largestExpenseAmount: Double,
    val largestExpenseCategory: String,
    val budgetLeft: Double?,
    val hasData: Boolean
) {
    fun cacheKey(): String {
        val top = topCategories.joinToString("|") { "${it.first}:${"%.0f".format(it.second)}" }
        val freq = frequentSmallExpenses.firstOrNull()?.let { "${it.name}:${it.count}" } ?: ""
        val month = "%.0f".format(monthExpense)
        return "home|$month|$top|$freq|${largestExpenseAmount.toLong()}|${budgetLeft?.toLong() ?: ""}"
    }
}

class HomeTipRepository(
    private val settingsRepository: SettingsRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(this@HomeTipRepository.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 8_000
        }
    }

    fun generateLocalTip(input: HomeTipInput): String {
        if (!input.hasData) return "先记一笔，慢慢看清消费。"

        // 预算少
        if (input.budgetLeft != null && input.budgetLeft < input.monthExpense * 0.2) {
            return "预算余量不多，今天轻一点。"
        }

        // TOP 分类明显最高（比第二名高 50% 以上）
        if (input.topCategories.size >= 2) {
            val top = input.topCategories[0]
            val second = input.topCategories[1]
            if (top.second > second.second * 1.5) {
                return "${top.first}花得较多，可稍微留意。"
            }
        }
        if (input.topCategories.size == 1) {
            val top = input.topCategories[0]
            return "${top.first}花得较多，可稍微留意。"
        }

        // 高频小额消费明显
        if (input.frequentSmallExpenses.isNotEmpty()) {
            val e = input.frequentSmallExpenses[0]
            return "${e.name}次数偏多，可少买一两次。"
        }

        // 最大单笔支出较高（超过日均 3 倍且大于 100 元）
        if (input.largestExpenseAmount > input.dailyAverage * 3 && input.largestExpenseAmount > 100) {
            return "${input.largestExpenseName}金额较高，记得确认。"
        }

        // 默认
        return "消费结构还平稳，继续记录。"
    }

    suspend fun generateTip(input: HomeTipInput): String {
        if (!input.hasData) return "还没有足够数据，先轻松记一笔吧。"

        val settings = settingsRepository.settingsFlow.first()
        val canUseAI = settings.useRealAI
                && settings.apiBaseUrl.isNotBlank()
                && settings.apiKey.isNotBlank()
                && settings.modelName.isNotBlank()

        if (canUseAI) {
            try {
                return callAI(input, settings.apiBaseUrl, settings.apiKey, settings.modelName)
            } catch (e: Exception) {
                Log.w("HomeTipRepo", "AI failed, using local fallback: ${e.message}")
            }
        }

        return generateLocalTip(input)
    }

    private suspend fun callAI(
        input: HomeTipInput,
        apiBaseUrl: String,
        apiKey: String,
        modelName: String
    ): String {
        val endpoint = apiBaseUrl.trimEnd('/') + "/chat/completions"
        val prompt = buildPrompt(input)

        val request = AIParseRequest(
            model = modelName,
            messages = listOf(
                AIMessage(
                    role = "system",
                    content = "你是记账 App 的首页消费提醒助手。请根据消费摘要输出一句具体账单分析和建议，15-30 个中文字符，最多不超过 40 字。关注分类、具体项目、频率、预算余量。不要写趋势判断。不要 Markdown、标题、列表。"
                ),
                AIMessage(role = "user", content = prompt)
            ),
            temperature = 0.3
        )

        val response: AIResponse = client.post(endpoint) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        response.error?.let {
            throw Exception("API error: ${it.message}")
        }

        val content = response.choices.firstOrNull()?.message?.content
            ?: throw Exception("Empty response")

        return cleanTip(content)
    }

    private fun buildPrompt(input: HomeTipInput): String {
        return buildString {
            append("本月总支出：${input.monthExpense.toLong()}。")
            append("今日支出：${input.todayExpense.toLong()}。")
            append("日均支出：${input.dailyAverage.toLong()}。")
            append("TOP分类：${input.topCategories.joinToString("、") { "${it.first}¥${it.second.toLong()}" }}。")
            if (input.frequentSmallExpenses.isNotEmpty()) {
                val freqStr = input.frequentSmallExpenses.joinToString("、") {
                    "${it.name}x${it.count}次/¥${it.amount.toLong()}"
                }
                append("高频小额消费：$freqStr。")
            }
            if (input.largestExpenseAmount > 0) {
                append("最大单笔支出：${input.largestExpenseName}¥${input.largestExpenseAmount.toLong()}。")
            }
            if (input.budgetLeft != null) {
                append("预算剩余：${input.budgetLeft.toLong()}。")
            }
        }
    }

    private fun cleanTip(text: String): String {
        return text
            .replace(Regex("^[#*\\->]+\\s*", RegexOption.MULTILINE), "")
            .replace("**", "")
            .replace("`", "")
            .replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("\\n{2,}"), "")
            .trim()
            .lines()
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("")
            .take(40)
    }

    fun cleanup() {
        client.close()
    }
}
