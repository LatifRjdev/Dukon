package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.repository.CustomerRepository

class GetCustomerPurchasesUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(storeId: String, customerId: String): Result<List<Sale>> = repository.getCustomerPurchases(storeId, customerId)
}
