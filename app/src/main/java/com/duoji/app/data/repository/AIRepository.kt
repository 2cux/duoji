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
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AIRepository(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Reason for falling back to local parsing after a failed remote AI attempt.
     * null means remote AI succeeded (no fallback).
     * Non-null means local fallback was used, with the specific reason.
     * Reset to null before each parse().
     */
    @Volatile
    var lastFallbackReason: String? = null
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
        lastFallbackReason = null
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("请输入记账内容"))
        }

        val settings = settingsRepository.settingsFlow.first()

        val canUseDeepSeek = settings.useRealAI
                && settings.apiBaseUrl.isNotBlank()
                && settings.apiKey.isNotBlank()
                && settings.modelName.isNotBlank()

        val maskedKey = if (settings.apiKey.isNotBlank()) {
            "sk-****${settings.apiKey.takeLast(4)}"
        } else {
            "empty"
        }

        Log.d("AIRepository", "parse: canUseDeepSeek=$canUseDeepSeek," +
                " useRealAI=${settings.useRealAI}," +
                " baseUrl=${settings.apiBaseUrl}," +
                " model=${settings.modelName}," +
                " apiKey=$maskedKey," +
                " inputLength=${input.length}")

        if (!canUseDeepSeek) {
            val reason = when {
                !settings.useRealAI -> "AI 配置未完成（开关未开启）"
                settings.apiKey.isBlank() -> "AI 配置未完成（API Key 为空）"
                settings.apiBaseUrl.isBlank() -> "AI 配置未完成（API Base URL 为空）"
                settings.modelName.isBlank() -> "AI 配置未完成（模型名称为空）"
                else -> "AI 配置未完成"
            }
            Log.w("AIRepository", "parse: 跳过远程 AI，使用本地解析。原因：$reason")
            lastFallbackReason = "AI 配置未完成"
            return parseLocal(input)
        }

        Log.d("AIRepository", "parse: 开始远程 AI 解析，inputLength=${input.length}")
        val deepSeekResult = parseWithDeepSeek(input, settings.apiBaseUrl, settings.apiKey, settings.modelName)
        if (deepSeekResult.isSuccess) {
            Log.d("AIRepository", "parse: 远程 AI 解析成功")
            return deepSeekResult
        }

        val exception = deepSeekResult.exceptionOrNull()
        val errorMsg = exception?.message ?: "未知错误"
        val fallbackReason = buildFallbackReason(errorMsg)
        lastFallbackReason = fallbackReason
        Log.w("AIRepository", "parse: 远程 AI 失败（$fallbackReason），降级到本地解析")
        return parseLocal(input)
    }

    /**
     * Map an error message from parseWithDeepSeek to a user-facing fallback reason shown in ConfirmScreen.
     */
    private fun buildFallbackReason(errorMsg: String): String = when {
        errorMsg.contains("API Key") || errorMsg.contains("api_key") ||
        errorMsg.contains("authentication") || errorMsg.contains("Unauthorized") ||
        errorMsg.contains("401") || errorMsg.contains("403") -> "API Key 可能无效，已使用本地解析"
        errorMsg.contains("网络连接") || errorMsg.contains("connect") ||
        errorMsg.contains("refused") || errorMsg.contains("UnknownHost") ||
        errorMsg.contains("econnrefused", ignoreCase = true) -> "网络连接失败，已使用本地解析"
        errorMsg.contains("超时") || errorMsg.contains("timeout") ||
        errorMsg.contains("timed out") -> "AI 响应超时，已使用本地解析"
        errorMsg.contains("格式异常") || errorMsg.contains("JSON") ||
        errorMsg.contains("Serialization") -> "AI 返回格式异常，已使用本地解析"
        errorMsg.contains("为空") || errorMsg.contains("未识别") ||
        errorMsg.contains("empty") -> "AI 未识别出账单，已使用本地解析"
        else -> "AI 识别暂时不可用（${errorMsg}），已使用本地解析"
    }

    private suspend fun parseWithDeepSeek(
        input: String,
        apiBaseUrl: String,
        apiKey: String,
        modelName: String
    ): Result<List<TransactionDraft>> {
        return try {
            val endpoint = apiBaseUrl.trimEnd('/') + "/chat/completions"
            Log.d("AIRepository", "parseWithDeepSeek: POST $endpoint, inputLength=${input.length}, model=$modelName")

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
                val errorDetail = error.message ?: "unknown"
                Log.e("AIRepository", "parseWithDeepSeek: API returned error=$errorDetail")
                val isAuthError = errorDetail.contains("Invalid API") ||
                        errorDetail.contains("api_key") ||
                        errorDetail.contains("authentication") ||
                        errorDetail.contains("401") ||
                        errorDetail.contains("403")
                return Result.failure(
                    if (isAuthError) AIException("API Key 可能无效")
                    else AIException("AI 服务返回错误：$errorDetail")
                )
            }

            val content = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(AIException("AI 返回为空"))

            Log.d("AIRepository", "parseWithDeepSeek: raw response content length=${content.length}")

            val cleaned = cleanJsonContent(content)
            Log.d("AIRepository", "parseWithDeepSeek: cleaned JSON=${cleaned.take(200)}")

            val parseResult = json.decodeFromString<AIParseResult>(cleaned)

            if (parseResult.transactions.isEmpty()) {
                Log.w("AIRepository", "parseWithDeepSeek: AI returned empty transactions")
                return Result.failure(AIException("AI 未识别出账单"))
            }

            val drafts = parseResult.transactions.map { it.toTransactionDraft() }
            Log.d("AIRepository", "parseWithDeepSeek: 成功解析 ${drafts.size} 笔账单")
            Result.success(drafts)

        } catch (e: kotlinx.serialization.SerializationException) {
            Log.e("AIRepository", "parseWithDeepSeek: JSON 解析失败", e)
            Result.failure(AIException("AI 返回格式异常"))
        } catch (e: HttpRequestTimeoutException) {
            Log.e("AIRepository", "parseWithDeepSeek: 请求超时", e)
            Result.failure(AIException("AI 响应超时"))
        } catch (e: SocketTimeoutException) {
            Log.e("AIRepository", "parseWithDeepSeek: Socket 超时", e)
            Result.failure(AIException("AI 响应超时"))
        } catch (e: ConnectException) {
            Log.e("AIRepository", "parseWithDeepSeek: 连接被拒绝", e)
            Result.failure(AIException("网络连接失败"))
        } catch (e: UnknownHostException) {
            Log.e("AIRepository", "parseWithDeepSeek: DNS 解析失败", e)
            Result.failure(AIException("网络连接失败"))
        } catch (e: Exception) {
            Log.e("AIRepository", "parseWithDeepSeek: 请求异常 ${e.javaClass.simpleName}: ${e.message}", e)
            val msg = e.message ?: ""
            when {
                msg.contains("401") || msg.contains("403") || msg.contains("Unauthorized") ||
                msg.contains("Invalid API") || msg.contains("authentication") ||
                msg.contains("api_key") -> Result.failure(AIException("API Key 可能无效"))
                msg.contains("timeout", ignoreCase = true) ||
                msg.contains("timed out", ignoreCase = true) -> Result.failure(AIException("AI 响应超时"))
                msg.contains("connect", ignoreCase = true) ||
                msg.contains("refused", ignoreCase = true) ||
                msg.contains("network", ignoreCase = true) ||
                msg.contains("econnrefused", ignoreCase = true) ||
                msg.contains("UnknownHost", ignoreCase = true) -> Result.failure(AIException("网络连接失败"))
                else -> Result.failure(AIException("DeepSeek 请求失败"))
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
