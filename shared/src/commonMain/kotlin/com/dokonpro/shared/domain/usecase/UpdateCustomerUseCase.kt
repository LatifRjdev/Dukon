package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.repository.CustomerRepository

class UpdateCustomerUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(customer: Customer): Result<Customer> = repository.updateCustomer(customer)
}
