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
    // Cloud (DeepSeek)
    private const val CLOUD_URL = "https://api.deepseek.com/v1/chat/completions"
    private const val API_KEY = "sk-30be22c5aefd49118f51306fb597c287"
    private const val CLOUD_MODEL = "deepseek-v4-pro"

    // Local (llama.cpp)
    private const val LOCAL_URL = "http://127.0.0.1:8080/v1/chat/completions"
    private const val LOCAL_MODEL = "qwen2.5-14b"

    /** Whether to prefer local model. Controlled by settings. */
    var useLocal: Boolean = false

    /** Check if local model is actually available (installed + server running). */
    fun isLocalAvailable(): Boolean = LocalModelManager.getStatus() is LocalModelManager.Status.Running

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
    }

    /**
     * Smart chat with automatic local/cloud routing.
     *
     * Priority rules:
     * 1. If useLocal=true AND local server is running → use local
     * 2. If useLocal=true but local not running → try to start it, fallback to cloud
     * 3. If useLocal=false → use cloud
     * 4. Cloud 401/402 → auto-switch to local if available
     */
    suspend fun chat(userMessage: String, fileHint: String? = null, fileContent: String? = null): String {
        // Determine which backend to use
        val (url, model, key) = resolveBackend()

        // Search intent handling (works with both backends)
        if (fileContent == null && fileHint == null && LocalSearchEngine.isSearchIntent(userMessage)) {
            return chatWithLocalSearch(userMessage, url, model, key)
        }

        var systemPrompt = "你是一个 AI 合作体平台的智能助手。回答要简洁直接，用中文。"
        if (fileHint != null) {
            systemPrompt += " 用户选中了文件「$fileHint」。"
            if (fileContent != null) {
                systemPrompt += " 以下是该文件的内容，请基于此内容回答用户问题：\n\n--- 文件 $fileHint 内容开始 ---\n$fileContent\n--- 文件内容结束 ---"
            }
        }
        return rawChat(systemPrompt, userMessage, url, model, key)
    }

    /** Resolve which backend to use based on settings and availability. */
    private fun resolveBackend(): Triple<String, String, String> {
        if (useLocal) {
            val status = LocalModelManager.getStatus()
            if (status is LocalModelManager.Status.Running) {
                return Triple(LOCAL_URL, LOCAL_MODEL, "")
            }
            if (status is LocalModelManager.Status.InstalledButNotRunning) {
                if (LocalModelManager.startServer()) {
                    return Triple(LOCAL_URL, LOCAL_MODEL, "")
                }
            }
        }
        return Triple(CLOUD_URL, CLOUD_MODEL, API_KEY)
    }

    /** Execute local search and inject results into prompt. */
    private suspend fun chatWithLocalSearch(userMessage: String, url: String, model: String, key: String): String {
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
        return rawChat(systemPrompt, userMessage, url, model, key)
    }

    private suspend fun rawChat(
        systemPrompt: String,
        userMessage: String,
        url: String,
        model: String,
        key: String,
    ): String {
        val backendLabel = if (url == CLOUD_URL) "云端(DeepSeek)" else "本地(qwen2.5:14b)"
        try {
            val httpResponse: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                if (key.isNotEmpty()) header("Authorization", "Bearer $key")
                setBody(ChatRequest(
                    model = model,
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
                val errorMsg = "API 错误 (${httpResponse.status.value}): $errBody"

                // Auto-fallback: cloud fails → try local
                if (url == CLOUD_URL && (httpResponse.status.value == 401 || httpResponse.status.value == 402)) {
                    if (isLocalAvailable()) {
                        return rawChat(systemPrompt, userMessage, LOCAL_URL, LOCAL_MODEL, "")
                    }
                }
                return errorMsg
            }
            val response: ChatResponse = httpResponse.body()
            val content = response.choices?.firstOrNull()?.message?.content ?: "（无回复）"
            return "[$backendLabel] $content"
        } catch (e: Exception) {
            val msg = e.message ?: "未知错误"
            if (msg.contains("402") || msg.contains("Insufficient Balance")) {
                if (isLocalAvailable()) {
                    return rawChat(systemPrompt, userMessage, LOCAL_URL, LOCAL_MODEL, "")
                }
                return "DeepSeek 账户余额不足，且本地模型不可用。"
            }
            // Connection refused → local server not running
            if (msg.contains("Connection refused") && url == LOCAL_URL) {
                return "本地模型未启动，请先在设置中点击「启动本地模型」。"
            }
            return "抱歉，请求出错了：$msg"
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
