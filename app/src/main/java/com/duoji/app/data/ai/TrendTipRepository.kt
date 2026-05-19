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

data class TrendSummaryInput(
    val range: String,
    val totalExpense: Double,
    val averageExpense: Double,
    val topDay: String,
    val topCategoryName: String,
    val topCategoryAmount: Double,
    val trendDirection: String,
    val budgetLeft: Double?,
    val hasData: Boolean
) {
    fun cacheKey(): String {
        return "$range|${"%.0f".format(totalExpense)}|$topCategoryName|$trendDirection|${budgetLeft?.toLong() ?: "none"}"
    }
}

class TrendTipRepository(
    private val settingsRepository: SettingsRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(this@TrendTipRepository.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 8_000
        }
    }

    fun generateLocalTip(input: TrendSummaryInput): String {
        if (!input.hasData) return "还没有足够数据，先轻松记一笔吧。"

        if (input.budgetLeft != null && input.budgetLeft >= 0) {
            return "本月还剩 ¥${input.budgetLeft.toLong()}，当前消费节奏还比较稳。"
        }

        if (input.budgetLeft != null && input.budgetLeft < 0) {
            return "这个月花得有点快，后面几天可以稍微收一收。"
        }

        if (input.topCategoryName.isNotBlank() && input.totalExpense > 0) {
            return "最近 ${input.topCategoryName} 花得比较多，可以稍微留意一下。"
        }

        if (input.trendDirection == "下降") {
            return "最近消费节奏比前几天平稳，继续保持。"
        }

        return "消费节奏整体平稳，继续保持记录就好。"
    }

    suspend fun generateTip(input: TrendSummaryInput): String {
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
                Log.w("TrendTipRepo", "AI failed, using local fallback: ${e.message}")
            }
        }

        return generateLocalTip(input)
    }

    private suspend fun callAI(
        input: TrendSummaryInput,
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
                    content = "你是一个记账 App 的轻量消费提醒助手。请根据统计摘要输出 1 句温和提醒，不要超过 60 个中文字符。不要使用 Markdown、标题、列表。必须结合具体分类、金额或趋势。不要制造焦虑。"
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

    private fun buildPrompt(input: TrendSummaryInput): String {
        return buildString {
            append("当前范围：${input.range}。")
            append("总支出：¥${input.totalExpense.toLong()}。")
            append("日均支出：¥${input.averageExpense.toLong()}。")
            append("最高消费日：${input.topDay}。")
            append("最高分类：${input.topCategoryName}，金额 ¥${input.topCategoryAmount.toLong()}。")
            append("趋势：${input.trendDirection}。")
            if (input.budgetLeft != null) {
                append("预算剩余：¥${input.budgetLeft.toLong()}。")
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
            .take(80)
    }

    fun cleanup() {
        client.close()
    }
}
