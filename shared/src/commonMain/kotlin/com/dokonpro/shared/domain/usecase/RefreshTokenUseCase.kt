package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.AuthTokens
import com.dokonpro.shared.domain.repository.AuthRepository

class RefreshTokenUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<AuthTokens> =
        repository.refreshToken()
}
