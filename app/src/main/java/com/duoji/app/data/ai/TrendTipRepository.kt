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
        if (!input.hasData) return "数据还不多，先继续记录。"

        if (input.trendDirection == "上升") return "近几天花得略快，注意节奏。"

        if (input.trendDirection == "下降") return "最近支出下降，节奏不错。"

        if (input.topDay.isNotBlank()) return "有一天支出偏高，可回看明细。"

        if (input.trendDirection == "平稳") return "消费节奏平稳，继续保持。"

        return "趋势整体平稳，继续观察。"
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
                    content = "你是记账 App 的趋势提醒助手。请根据趋势摘要输出一句趋势分析和建议，15-30 个中文字符，最多不超过 40 字。关注消费上升、下降、平稳、某天偏高。不要写具体账单分类分析。不要 Markdown、标题、列表。"
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
            if (input.topDay.isNotBlank()) {
                append("最高日：${input.topDay}。")
            }
            append("趋势：${input.trendDirection}。")
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
            .firstOrNull()
            .orEmpty()
            .take(40)
    }

    fun cleanup() {
        client.close()
    }
}
