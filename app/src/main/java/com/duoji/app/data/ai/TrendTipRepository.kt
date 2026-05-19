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
        if (!input.hasData) return "先继续记录，慢慢看清趋势。"

        when (input.trendDirection) {
            "上升" -> return "近几天支出略有上升，留意节奏。"
            "下降" -> return "近几天支出放缓，节奏不错。"
            "平稳" -> return "本周消费节奏比较平稳。"
        }

        return "消费节奏还比较平稳。"
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
                    content = "你是记账 App 的轻量趋势提醒助手。请根据支出趋势摘要输出一句温和提醒，15-30 个中文字符，最多 40 字。只描述整体走势，如上升、下降、平稳、波动，不要提具体商品、商户或单笔账单。可以只做中性观察，不一定给建议。不要说教，不要制造焦虑，不要 Markdown。"
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
            append("范围：${input.range}。")
            append("总支出：¥${input.totalExpense.toLong()}。")
            append("日均：¥${input.averageExpense.toLong()}。")
            append("趋势：${input.trendDirection}。")
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
            .firstOrNull()
            .orEmpty()
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
