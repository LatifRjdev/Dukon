package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow

class SearchCustomerUseCase(private val repository: CustomerRepository) {
    operator fun invoke(storeId: String, query: String): Flow<List<Customer>> = repository.searchCustomers(storeId, query)
}
