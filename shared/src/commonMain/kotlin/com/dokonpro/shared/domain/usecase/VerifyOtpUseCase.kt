package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.repository.AuthRepository
import com.dokonpro.shared.domain.repository.VerifyOtpResult

class VerifyOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(phone: String, code: String): Result<VerifyOtpResult> =
        repository.verifyOtp(phone, code)
}
