package com.duoji.app.data.ai

import android.util.Log
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

        Log.d("MonthlyAdviceRepo", "generateAdvice: canUseDeepSeek=$canUseDeepSeek, useRealAI=${settings.useRealAI}, " +
                "apiKey=${if (settings.apiKey.isNotBlank()) "***" else "empty"}")

        if (canUseDeepSeek) {
            try {
                val result = generateWithDeepSeek(statistics, settings.apiBaseUrl, settings.apiKey, settings.modelName)
                Log.d("MonthlyAdviceRepo", "generateAdvice: DeepSeek 成功, length=${result.length}")
                return result
            } catch (e: Exception) {
                Log.e("MonthlyAdviceRepo", "generateAdvice: DeepSeek 失败 ${e.javaClass.simpleName}: ${e.message}", e)
            }
        }
        val local = LocalMonthlyAdviceGenerator.generate(statistics)
        Log.d("MonthlyAdviceRepo", "generateAdvice: 使用本地生成, length=${local.length}")
        return local
    }

    private suspend fun generateWithDeepSeek(
        statistics: MonthlyStatistics,
        apiBaseUrl: String,
        apiKey: String,
        modelName: String
    ): String {
        val endpoint = apiBaseUrl.trimEnd('/') + "/chat/completions"
        val prompt = MonthlyAdvicePromptBuilder.buildPrompt(statistics)

        Log.d("MonthlyAdviceRepo", "generateWithDeepSeek: POST $endpoint, model=$modelName")

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
            Log.e("MonthlyAdviceRepo", "generateWithDeepSeek: API error=${error.message}")
            throw Exception("API 错误：${error.message}")
        }

        val content = response.choices.firstOrNull()?.message?.content
        if (content == null) {
            Log.e("MonthlyAdviceRepo", "generateWithDeepSeek: 返回内容为空")
            throw Exception("AI 返回为空")
        }

        Log.d("MonthlyAdviceRepo", "generateWithDeepSeek: 成功, content length=${content.length}")
        return content
    }

    fun cleanup() {
        client.close()
    }
}
