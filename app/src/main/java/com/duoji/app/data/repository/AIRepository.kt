package com.duoji.app.data.repository

import com.duoji.app.data.ai.LocalMockParser
import com.duoji.app.data.ai.PromptBuilder
import com.duoji.app.data.model.*
import com.duoji.app.data.settings.SettingsRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class AIRepository(
    private val settingsRepository: SettingsRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(this@AIRepository.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    /**
     * Parse natural language input into structured transactions.
     * Uses DeepSeek if configured and enabled, otherwise falls back to LocalMockParser.
     */
    suspend fun parse(input: String): Result<List<TransactionDraft>> {
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("请输入记账内容"))
        }

        val settings = settingsRepository.settingsFlow.first()

        // Check if DeepSeek should be used
        val canUseDeepSeek = settings.useRealAI
                && settings.apiBaseUrl.isNotBlank()
                && settings.apiKey.isNotBlank()
                && settings.modelName.isNotBlank()

        if (!canUseDeepSeek) {
            return parseLocal(input)
        }

        // Try DeepSeek, fallback to local on any failure
        val deepSeekResult = parseWithDeepSeek(input, settings.apiBaseUrl, settings.apiKey, settings.modelName)
        if (deepSeekResult.isSuccess) {
            return deepSeekResult
        }

        return parseLocal(input)
    }

    private suspend fun parseWithDeepSeek(
        input: String,
        apiBaseUrl: String,
        apiKey: String,
        modelName: String
    ): Result<List<TransactionDraft>> {
        return try {
            val endpoint = apiBaseUrl.trimEnd('/') + "/chat/completions"
            val systemPrompt = PromptBuilder.buildSystemPrompt()
            val userPrompt = PromptBuilder.buildUserPrompt(input)

            val request = AIParseRequest(
                model = modelName,
                messages = listOf(
                    AIMessage(role = "system", content = systemPrompt),
                    AIMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.2
            )

            val response: AIResponse = client.post(endpoint) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            response.error?.let { error ->
                return Result.failure(AIException("API 错误，已切回本地解析"))
            }

            val content = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(AIException("AI 返回为空，已切回本地解析"))

            val cleaned = cleanJsonContent(content)

            val parseResult = json.decodeFromString<AIParseResult>(cleaned)

            if (parseResult.transactions.isEmpty()) {
                return Result.failure(AIException("未能识别出账单，已切回本地解析"))
            }

            val drafts = parseResult.transactions.map { it.toTransactionDraft() }
            Result.success(drafts)

        } catch (e: Exception) {
            if (e is AIException) {
                Result.failure(e)
            } else {
                Result.failure(AIException("DeepSeek 请求失败，已切回本地解析"))
            }
        }
    }

    private fun parseLocal(input: String): Result<List<TransactionDraft>> {
        return try {
            val results = LocalMockParser.parseMulti(input)
            if (results.isEmpty()) {
                return Result.failure(AIException("未能识别出账单，请重新描述"))
            }
            val drafts = results.map { it.toTransactionDraft() }
            Result.success(drafts)
        } catch (e: Exception) {
            Result.failure(AIException("本地解析失败，可以手动记一笔。"))
        }
    }

    fun cleanup() {
        client.close()
    }
}

class AIException(message: String) : Exception(message)

/**
 * Clean JSON string from markdown code blocks and surrounding text.
 * Handles ```json, ``` wrapping, and extracts JSON from text.
 */
fun cleanJsonContent(content: String): String {
    var trimmed = content.trim()

    // Remove leading ```json
    if (trimmed.startsWith("```json")) {
        trimmed = trimmed.removePrefix("```json").trim()
    }
    // Remove leading ```
    if (trimmed.startsWith("```")) {
        trimmed = trimmed.removePrefix("```").trim()
    }
    // Remove trailing ```
    if (trimmed.endsWith("```")) {
        trimmed = trimmed.removeSuffix("```").trim()
    }

    // Extract JSON between first { and last }
    val start = trimmed.indexOf('{')
    val end = trimmed.lastIndexOf('}')

    if (start == -1 || end == -1 || start >= end) {
        throw IllegalArgumentException("找不到有效的 JSON")
    }

    return trimmed.substring(start, end + 1)
}
