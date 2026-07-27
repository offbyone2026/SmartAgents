package com.smartagents.desktop

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ChatApi {
    private const val API_URL = "https://api.deepseek.com/v1/chat/completions"
    private const val API_KEY = "sk-30be22c5aefd49118f51306fb597c287"
    private const val MODEL = "deepseek-v4-pro"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
    }

    /**
     * Smart chat: detects file-search intent in user message, runs local search,
     * and injects results into the prompt before calling DeepSeek.
     */
    suspend fun chat(userMessage: String, fileHint: String? = null, fileContent: String? = null): String {
        // Step 1: Check if user is asking for a local file search
        if (fileContent == null && fileHint == null && LocalSearchEngine.isSearchIntent(userMessage)) {
            return chatWithLocalSearch(userMessage)
        }

        // Step 2: Regular chat (possibly with attached file content)
        var systemPrompt = "你是 SmartAgents，一个 AI 合作体平台的智能助手。回答要简洁直接，用中文。"
        if (fileHint != null) {
            systemPrompt += " 用户选中了文件「$fileHint」。"
            if (fileContent != null) {
                systemPrompt += " 以下是该文件的内容，请基于此内容回答用户问题：\n\n--- 文件 $fileHint 内容开始 ---\n$fileContent\n--- 文件内容结束 ---"
            }
        }
        return rawChat(systemPrompt, userMessage)
    }

    /** Execute local search and ask DeepSeek to present results. */
    private suspend fun chatWithLocalSearch(userMessage: String): String {
        val params = LocalSearchEngine.parseParams(userMessage)
        val results = LocalSearchEngine.search(params)
        val searchReport = LocalSearchEngine.formatResults(params, results)

        val systemPrompt = buildString {
            appendLine("你是 SmartAgents，一个 AI 合作体平台的智能助手。回答要简洁直接，用中文。")
            appendLine()
            appendLine("用户请求了本地文件搜索。以下是在用户电脑上实际找到的结果：")
            appendLine(searchReport)
            appendLine()
            appendLine("请用自然语言向用户汇报搜索结果，列出关键文件。如果结果很多，挑选最重要/最大的几个展示，并说明总共找到多少个。")
        }
        return rawChat(systemPrompt, userMessage)
    }

    private suspend fun rawChat(systemPrompt: String, userMessage: String): String {
        try {
            val httpResponse: HttpResponse = client.post(API_URL) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $API_KEY")
                setBody(ChatRequest(
                    model = MODEL,
                    messages = listOf(
                        ChatMsg("system", systemPrompt),
                        ChatMsg("user", userMessage)
                    ),
                    max_tokens = 2048,
                    temperature = 0.7,
                ))
            }
            if (httpResponse.status != HttpStatusCode.OK) {
                val errBody = httpResponse.bodyAsText()
                return "API 错误 (${httpResponse.status.value}): $errBody"
            }
            val response: ChatResponse = httpResponse.body()
            return response.choices?.firstOrNull()?.message?.content ?: "（无回复）"
        } catch (e: Exception) {
            if (e.message?.contains("402") == true || e.message?.contains("Insufficient Balance") == true) {
                return "DeepSeek 账户余额不足，请充值后重试。"
            }
            return "抱歉，请求出错了：${e.message ?: "未知错误"}"
        }
    }
}

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMsg>,
    val max_tokens: Int = 2048,
    val temperature: Double = 0.7,
)

@Serializable
data class ChatMsg(val role: String, val content: String)

@Serializable
data class ChatResponse(val choices: List<ChatChoice>? = null)

@Serializable
data class ChatChoice(val message: ChatMsg? = null)
