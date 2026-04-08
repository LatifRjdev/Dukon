package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.FinanceSummary
import com.dokonpro.shared.domain.repository.FinanceRepository

class GetFinanceSummaryUseCase(private val repository: FinanceRepository) {
    suspend operator fun invoke(storeId: String, period: String = "day"): Result<FinanceSummary> =
        repository.getSummary(storeId, period)
}
