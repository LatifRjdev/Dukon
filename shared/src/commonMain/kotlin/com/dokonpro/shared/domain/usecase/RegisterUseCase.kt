package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.repository.AuthRepository
import com.dokonpro.shared.domain.repository.RegisterResult

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(phone: String, name: String, storeName: String): Result<RegisterResult> =
        repository.register(phone, name, storeName)
}
