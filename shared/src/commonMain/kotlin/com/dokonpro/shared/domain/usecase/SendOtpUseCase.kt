package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.repository.AuthRepository

class SendOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(phone: String): Result<Unit> =
        repository.sendOtp(phone)
}
