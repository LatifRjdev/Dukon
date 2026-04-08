package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.ZakatCalculation
import com.dokonpro.shared.domain.repository.ZakatRepository
import kotlinx.coroutines.flow.Flow

class GetZakatHistoryUseCase(private val repository: ZakatRepository) {
    operator fun invoke(storeId: String): Flow<List<ZakatCalculation>> =
        repository.getHistory(storeId)
}
