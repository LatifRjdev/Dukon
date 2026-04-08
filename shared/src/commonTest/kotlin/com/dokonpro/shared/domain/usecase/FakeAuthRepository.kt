package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.AuthTokens
import com.dokonpro.shared.domain.entity.Store
import com.dokonpro.shared.domain.entity.User
import com.dokonpro.shared.domain.repository.AuthRepository
import com.dokonpro.shared.domain.repository.RegisterResult
import com.dokonpro.shared.domain.repository.VerifyOtpResult

class FakeAuthRepository : AuthRepository {
    var sendOtpResult: Result<Unit> = Result.success(Unit)
    var verifyOtpResult: Result<VerifyOtpResult> = Result.success(
        VerifyOtpResult(
            tokens = AuthTokens(accessToken = "test-access", refreshToken = "test-refresh"),
            isNewUser = false
        )
    )
    var registerResult: Result<RegisterResult> = Result.success(
        RegisterResult(
            tokens = AuthTokens(accessToken = "test-access", refreshToken = "test-refresh"),
            user = User(id = "user-1", phone = "+992901234567", name = "Test"),
            store = Store(id = "store-1", name = "Test Store")
        )
    )
    var refreshResult: Result<AuthTokens> = Result.success(
        AuthTokens(accessToken = "new-access", refreshToken = "new-refresh")
    )

    var lastSentPhone: String? = null
    var lastVerifyPhone: String? = null
    var lastVerifyCode: String? = null
    var loggedOut = false

    override suspend fun sendOtp(phone: String): Result<Unit> {
        lastSentPhone = phone
        return sendOtpResult
    }

    override suspend fun verifyOtp(phone: String, code: String): Result<VerifyOtpResult> {
        lastVerifyPhone = phone
        lastVerifyCode = code
        return verifyOtpResult
    }

    override suspend fun register(phone: String, name: String, storeName: String): Result<RegisterResult> =
        registerResult

    override suspend fun refreshToken(): Result<AuthTokens> = refreshResult

    override suspend fun logout() { loggedOut = true }

    override suspend fun isLoggedIn(): Boolean = !loggedOut
}
