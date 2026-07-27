package com.smartagents.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val success: Boolean, val token: String? = null, val message: String? = null)

@Serializable
data class VerifyRequest(val token: String)

@Serializable
data class VerifyResponse(val success: Boolean, val username: String? = null, val message: String? = null)

@Serializable
data class AuthState(val token: String, val username: String)
