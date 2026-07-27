package com.smartagents.desktop

import java.io.File
import kotlinx.coroutines.*

/**
 * Manages the local llama.cpp inference server.
 *
 * - Checks if llama-server + GGUF model exist
 * - Starts/stops the server
 * - Reports status to UI
 */
object LocalModelManager {

    private val baseDir = File(System.getenv("LOCALAPPDATA"), "SmartAgents\\llm")
    private val serverExe = File(baseDir, "llama-server.exe")
    private val modelDir = File(baseDir, "models")
    private val modelFile = File(modelDir, "Qwen2.5-14B-Instruct-Q4_K_M.gguf")

    private var serverProcess: Process? = null

    sealed class Status {
        data object NotInstalled : Status()
        data object InstalledButNotRunning : Status()
        data object Running : Status()
        data class Error(val message: String) : Status()
    }

    fun getStatus(): Status {
        if (!serverExe.exists() || !modelFile.exists()) {
            return Status.NotInstalled
        }
        return try {
            val proc = ProcessBuilder("tasklist.exe").start()
            val output = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            if (output.contains("llama-server.exe")) Status.Running else Status.InstalledButNotRunning
        } catch (e: Exception) {
            Status.Error(e.message ?: "Unknown")
        }
    }

    fun getModelPath(): String = modelFile.absolutePath

    fun getEndpoint(): String = "http://127.0.0.1:8080/v1/chat/completions"

    fun startServer(): Boolean {
        if (getStatus() is Status.Running) return true
        if (!serverExe.exists() || !modelFile.exists()) return false

        return try {
            serverProcess = ProcessBuilder(
                serverExe.absolutePath,
                "-m", modelFile.absolutePath,
                "--host", "127.0.0.1",
                "--port", "8080",
                "-ngl", "99",
                "-c", "4096"
            )
                .directory(baseDir)
                .redirectErrorStream(true)
                .start()
            // Give it a moment to load
            Thread.sleep(2000)
            serverProcess?.isAlive ?: false
        } catch (e: Exception) {
            Status.Error(e.message ?: "Failed to start")
            false
        }
    }

    fun stopServer() {
        serverProcess?.let {
            it.destroy()
            serverProcess = null
        }
    }
}
