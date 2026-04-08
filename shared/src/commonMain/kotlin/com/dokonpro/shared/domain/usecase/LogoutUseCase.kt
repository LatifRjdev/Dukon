package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.repository.AuthRepository

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.logout()
}
