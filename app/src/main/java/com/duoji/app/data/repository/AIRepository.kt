package com.duoji.app.data.repository

import android.util.Log
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
    /**
     * Whether the most recent successful parse() call fell back to local mock parsing.
     * Reset to false before each parse(), set to true when local fallback is used.
     * Only meaningful immediately after parse() returns successfully.
     */
    @Volatile
    var lastResultUsedLocalFallback: Boolean = false
        private set
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
        lastResultUsedLocalFallback = false
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("请输入记账内容"))
        }

        val settings = settingsRepository.settingsFlow.first()

        val canUseDeepSeek = settings.useRealAI
                && settings.apiBaseUrl.isNotBlank()
                && settings.apiKey.isNotBlank()
                && settings.modelName.isNotBlank()

        Log.d("AIRepository", "parse: canUseDeepSeek=$canUseDeepSeek, useRealAI=${settings.useRealAI}, " +
                "apiKey=${if (settings.apiKey.isNotBlank()) "***${settings.apiKey.takeLast(4)}" else "empty"}, " +
                "model=${settings.modelName}, baseUrl=${settings.apiBaseUrl}")

        if (!canUseDeepSeek) {
            val reason = when {
                !settings.useRealAI -> "useRealAI 未开启"
                settings.apiKey.isBlank() -> "API Key 为空"
                settings.apiBaseUrl.isBlank() -> "API Base URL 为空"
                settings.modelName.isBlank() -> "模型名称为空"
                else -> "未知原因"
            }
            Log.w("AIRepository", "parse: 使用本地解析（原因：$reason）")
            lastResultUsedLocalFallback = true
            return parseLocal(input)
        }

        // Try DeepSeek, fallback to local on any failure
        val deepSeekResult = parseWithDeepSeek(input, settings.apiBaseUrl, settings.apiKey, settings.modelName)
        if (deepSeekResult.isSuccess) {
            Log.d("AIRepository", "parse: DeepSeek 解析成功")
            return deepSeekResult
        }

        val errorMsg = deepSeekResult.exceptionOrNull()?.message ?: "未知错误"
        Log.w("AIRepository", "parse: DeepSeek 失败（$errorMsg），降级到本地解析")
        lastResultUsedLocalFallback = true
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
            Log.d("AIRepository", "parseWithDeepSeek: POST $endpoint (input=\"$input\")")

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
                Log.e("AIRepository", "parseWithDeepSeek: API returned error=${error.message}")
                return Result.failure(AIException("API 错误：${error.message}，已切回本地解析"))
            }

            val content = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(AIException("AI 返回为空，已切回本地解析"))

            Log.d("AIRepository", "parseWithDeepSeek: raw response content length=${content.length}")

            val cleaned = cleanJsonContent(content)
            Log.d("AIRepository", "parseWithDeepSeek: cleaned JSON=${cleaned.take(200)}")

            val parseResult = json.decodeFromString<AIParseResult>(cleaned)

            if (parseResult.transactions.isEmpty()) {
                Log.w("AIRepository", "parseWithDeepSeek: AI returned empty transactions")
                return Result.failure(AIException("未能识别出账单，已切回本地解析"))
            }

            val drafts = parseResult.transactions.map { it.toTransactionDraft() }
            Log.d("AIRepository", "parseWithDeepSeek: 成功解析 ${drafts.size} 笔账单")
            Result.success(drafts)

        } catch (e: kotlinx.serialization.SerializationException) {
            Log.e("AIRepository", "parseWithDeepSeek: JSON 解析失败", e)
            Result.failure(AIException("API 返回格式异常，已切回本地解析"))
        } catch (e: Exception) {
            Log.e("AIRepository", "parseWithDeepSeek: 请求异常 ${e.javaClass.simpleName}: ${e.message}", e)
            if (e is AIException) {
                Result.failure(e)
            } else {
                Result.failure(AIException("DeepSeek 请求失败，已切回本地解析"))
            }
        }
    }

    private fun parseLocal(input: String): Result<List<TransactionDraft>> {
        Log.d("AIRepository", "parseLocal: input=\"$input\"")
        return try {
            val results = LocalMockParser.parseMulti(input)
            if (results.isEmpty()) {
                Log.w("AIRepository", "parseLocal: 未能识别出账单")
                return Result.failure(AIException("未能识别出账单，请重新描述"))
            }
            val drafts = results.map { it.toTransactionDraft() }
            Log.d("AIRepository", "parseLocal: 解析到 ${drafts.size} 笔账单, categories=${drafts.map { it.category }}")
            Result.success(drafts)
        } catch (e: Exception) {
            Log.e("AIRepository", "parseLocal: 本地解析异常", e)
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
