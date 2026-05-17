package com.duoji.app.data.ai

import com.duoji.app.data.model.*
import com.duoji.app.data.settings.SettingsRepository
import com.duoji.app.domain.statistics.MonthlyStatistics
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

class MonthlyAdviceRepository(
    private val settingsRepository: SettingsRepository
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
        val settings = settingsRepository.settingsFlow.first()

        val canUseDeepSeek = settings.useRealAI
                && settings.apiBaseUrl.isNotBlank()
                && settings.apiKey.isNotBlank()
                && settings.modelName.isNotBlank()

        if (canUseDeepSeek) {
            try {
                return generateWithDeepSeek(statistics, settings.apiBaseUrl, settings.apiKey, settings.modelName)
            } catch (_: Exception) {
                // Fallback to local
            }
        }
        return LocalMonthlyAdviceGenerator.generate(statistics)
    }

    private suspend fun generateWithDeepSeek(
        statistics: MonthlyStatistics,
        apiBaseUrl: String,
        apiKey: String,
        modelName: String
    ): String {
        val endpoint = apiBaseUrl.trimEnd('/') + "/chat/completions"
        val prompt = MonthlyAdvicePromptBuilder.buildPrompt(statistics)

        val request = AIParseRequest(
            model = modelName,
            messages = listOf(
                AIMessage(role = "user", content = prompt)
            )
        )

        val response: AIResponse = client.post(endpoint) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        response.error?.let { error ->
            throw Exception("API 错误")
        }

        return response.choices.firstOrNull()?.message?.content
            ?: throw Exception("AI 返回为空")
    }

    fun cleanup() {
        client.close()
    }
}
