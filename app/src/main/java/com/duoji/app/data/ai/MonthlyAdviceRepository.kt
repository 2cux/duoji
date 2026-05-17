package com.duoji.app.data.ai

import com.duoji.app.data.model.*
import com.duoji.app.domain.statistics.MonthlyStatistics
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class MonthlyAdviceRepository(
    private val apiKey: String = "",
    private val apiBaseUrl: String = "https://api.openai.com/v1/chat/completions",
    private val model: String = "gpt-4o-mini"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(this@MonthlyAdviceRepository.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun generateAdvice(statistics: MonthlyStatistics): String {
        if (apiKey.isNotBlank()) {
            try {
                return generateWithAI(statistics)
            } catch (_: Exception) {
                // Fallback to local
            }
        }
        return LocalMonthlyAdviceGenerator.generate(statistics)
    }

    private suspend fun generateWithAI(statistics: MonthlyStatistics): String {
        val prompt = MonthlyAdvicePromptBuilder.buildPrompt(statistics)

        val request = AIParseRequest(
            model = model,
            messages = listOf(
                AIMessage(role = "user", content = prompt)
            )
        )

        val response: AIResponse = client.post(apiBaseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        response.error?.let { error ->
            throw Exception("API 错误: ${error.message}")
        }

        return response.choices.firstOrNull()?.message?.content
            ?: throw Exception("AI 返回为空")
    }

    fun cleanup() {
        client.close()
    }
}
