package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.ZakatCalculation
import com.dokonpro.shared.domain.repository.ZakatRepository

class CalculateZakatUseCase(private val repository: ZakatRepository) {
    suspend operator fun invoke(storeId: String): Result<ZakatCalculation> =
        repository.calculate(storeId)
}
