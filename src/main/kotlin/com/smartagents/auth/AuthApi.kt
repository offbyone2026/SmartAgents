package com.smartagents.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File

object AuthApi {
    private const val BASE_URL = "http://82.156.111.14/api"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun login(username: String, password: String): LoginResponse {
        return client.post("$BASE_URL/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }.body()
    }

    suspend fun verify(token: String): VerifyResponse {
        return client.post("$BASE_URL/verify") {
            contentType(ContentType.Application.Json)
            setBody(VerifyRequest(token))
        }.body()
    }
}

object AuthStorage {
    private val authDir = File(System.getProperty("user.home"), ".smartagents")
    private val authFile = File(authDir, "auth.json")

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AuthState? {
        if (!authFile.exists()) return null
        return try {
            json.decodeFromString<AuthState>(authFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun save(state: AuthState) {
        authDir.mkdirs()
        authFile.writeText(json.encodeToString(AuthState.serializer(), state))
    }

    fun clear() {
        authFile.delete()
    }
}
