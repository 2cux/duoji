package com.duoji.app.data.repository

import com.duoji.app.data.ai.LocalMockParser
import com.duoji.app.data.ai.PromptBuilder
import com.duoji.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class AIRepository(
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
            json(this@AIRepository.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    /**
     * Parse natural language input into structured transactions.
     * Falls back to LocalMockParser if no API key is configured.
     */
    suspend fun parse(input: String): Result<List<TransactionDraft>> {
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("请输入记账内容"))
        }

        // Fallback to local mock parser when no API key
        if (apiKey.isBlank()) {
            return parseLocal(input)
        }

        return parseWithAI(input)
    }

    private suspend fun parseWithAI(input: String): Result<List<TransactionDraft>> {
        return try {
            val systemPrompt = PromptBuilder.buildSystemPrompt()
            val userPrompt = PromptBuilder.buildUserPrompt(input)

            val request = AIParseRequest(
                model = model,
                messages = listOf(
                    AIMessage(role = "system", content = systemPrompt),
                    AIMessage(role = "user", content = userPrompt)
                )
            )

            val response: AIResponse = client.post(apiBaseUrl) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            response.error?.let { error ->
                return Result.failure(AIException("API 错误: ${error.message}"))
            }

            val content = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(AIException("AI 返回为空，请重试"))

            val cleaned = content
                .replace(Regex("""^```json\s*"""), "")
                .replace(Regex("""\s*```$"""), "")
                .trim()

            val parseResult = json.decodeFromString<AIParseResult>(cleaned)

            if (parseResult.transactions.isEmpty()) {
                return Result.failure(AIException("未能识别出账单，请重新描述"))
            }

            val drafts = parseResult.transactions.map { it.toTransactionDraft() }
            Result.success(drafts)

        } catch (e: Exception) {
            if (e is AIException) {
                Result.failure(e)
            } else {
                Result.failure(AIException("识别失败，可手动记账"))
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
            Result.failure(AIException("本地解析失败: ${e.message}"))
        }
    }

    fun cleanup() {
        client.close()
    }
}

class AIException(message: String) : Exception(message)
