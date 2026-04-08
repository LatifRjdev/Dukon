package com.dokonpro.shared.domain.repository

import com.dokonpro.shared.domain.entity.AuthTokens
import com.dokonpro.shared.domain.entity.Store
import com.dokonpro.shared.domain.entity.User

interface AuthRepository {
    suspend fun sendOtp(phone: String): Result<Unit>
    suspend fun verifyOtp(phone: String, code: String): Result<VerifyOtpResult>
    suspend fun register(phone: String, name: String, storeName: String): Result<RegisterResult>
    suspend fun refreshToken(): Result<AuthTokens>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
}

data class VerifyOtpResult(
    val tokens: AuthTokens,
    val isNewUser: Boolean
)

data class RegisterResult(
    val tokens: AuthTokens,
    val user: User,
    val store: Store
)
