package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow

class GetSalesHistoryUseCase(private val repository: SaleRepository) {
    operator fun invoke(storeId: String): Flow<List<Sale>> = repository.getSalesHistory(storeId)
}
