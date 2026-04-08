package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.repository.CustomerRepository

class CreateCustomerUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(storeId: String, customer: Customer): Result<Customer> = repository.createCustomer(storeId, customer)
}
