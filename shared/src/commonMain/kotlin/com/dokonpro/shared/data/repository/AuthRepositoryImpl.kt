package com.dokonpro.shared.data.repository

import com.dokonpro.shared.data.local.TokenStorage
import com.dokonpro.shared.data.remote.AuthApiClient
import com.dokonpro.shared.domain.entity.AuthTokens
import com.dokonpro.shared.domain.entity.Store
import com.dokonpro.shared.domain.entity.User
import com.dokonpro.shared.domain.repository.AuthRepository
import com.dokonpro.shared.domain.repository.RegisterResult
import com.dokonpro.shared.domain.repository.VerifyOtpResult

class AuthRepositoryImpl(
    private val api: AuthApiClient,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun sendOtp(phone: String): Result<Unit> = runCatching {
        api.sendOtp(phone)
    }

    override suspend fun verifyOtp(phone: String, code: String): Result<VerifyOtpResult> = runCatching {
        val response = api.verifyOtp(phone, code)
        val tokens = AuthTokens(response.accessToken, response.refreshToken)
        if (!response.isNewUser) {
            tokenStorage.saveTokens(tokens)
        }
        VerifyOtpResult(tokens = tokens, isNewUser = response.isNewUser)
    }

    override suspend fun register(phone: String, name: String, storeName: String): Result<RegisterResult> = runCatching {
        val response = api.register(phone, name, storeName)
        val tokens = AuthTokens(response.accessToken, response.refreshToken)
        tokenStorage.saveTokens(tokens)
        RegisterResult(
            tokens = tokens,
            user = User(id = response.user.id, phone = response.user.phone, name = response.user.name),
            store = Store(id = response.store.id, name = response.store.name)
        )
    }

    override suspend fun refreshToken(): Result<AuthTokens> = runCatching {
        val current = tokenStorage.getTokens() ?: throw IllegalStateException("No tokens stored")
        val response = api.refresh(current.refreshToken)
        val tokens = AuthTokens(response.accessToken, response.refreshToken)
        tokenStorage.saveTokens(tokens)
        tokens
    }

    override suspend fun logout() {
        tokenStorage.clearTokens()
    }

    override suspend fun isLoggedIn(): Boolean = tokenStorage.hasTokens()
}
