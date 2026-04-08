package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.DailyRevenue
import com.dokonpro.shared.domain.repository.FinanceRepository

class GetReportUseCase(private val repository: FinanceRepository) {
    suspend operator fun invoke(storeId: String, period: String = "week"): Result<List<DailyRevenue>> =
        repository.getReport(storeId, period)
}
