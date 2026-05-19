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

        // 预算还有余量
        if (input.budgetLeft != null && input.budgetLeft > 0) {
            return "预算还有余量，保持当前节奏。"
        }

        // TOP 分类明显最高（比第二名高 50% 以上）
        if (input.topCategories.size >= 2) {
            val top = input.topCategories[0]
            val second = input.topCategories[1]
            if (top.second > second.second * 1.5) {
                return "本月${top.first}占比较高，可以稍微留意。"
            }
        }
        if (input.topCategories.size == 1) {
            val top = input.topCategories[0]
            return "本月${top.first}占比较高，可以稍微留意。"
        }

        // 默认
        return "消费节奏还比较平稳。"
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
                    content = "你是记账 App 的轻量消费提醒助手。请根据消费分类摘要输出一句温和提醒，15-30 个中文字符，最多 40 字。只分析类别，如餐饮、购物、交通等，不要提具体商品、商户或单笔账单。可以只做中性观察，不一定给建议。不要说教，不要制造焦虑，不要 Markdown。"
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
            if (input.budgetLeft != null) {
                append("预算剩余：${input.budgetLeft.toLong()}。")
            }
        }
    }

    private val sensitiveWords = listOf(
        "少买", "不要再", "控制", "花太多", "不必要",
        "异常", "超支严重", "必须", "应该", "警告"
    )

    private fun cleanTip(text: String): String {
        val cleaned = text
            .replace(Regex("^[#*\->]+\s*", RegexOption.MULTILINE), "")
            .replace("**", "")
            .replace("`", "")
            .replace(Regex("```[\s\S]*?```"), "")
            .replace(Regex("\n{2,}"), "")
            .trim()
            .lines()
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("")
            .take(40)

        // Sensitive word check
        if (sensitiveWords.any { cleaned.contains(it) }) {
            return ""
        }
        return cleaned
    }
    fun cleanup() {
        client.close()
    }
}
