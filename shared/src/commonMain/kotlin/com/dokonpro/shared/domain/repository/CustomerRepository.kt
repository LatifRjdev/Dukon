package com.dokonpro.shared.domain.repository

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.entity.Sale
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun getCustomers(storeId: String): Flow<List<Customer>>
    fun searchCustomers(storeId: String, query: String): Flow<List<Customer>>
    suspend fun getCustomerById(id: String): Customer?
    suspend fun createCustomer(storeId: String, customer: Customer): Result<Customer>
    suspend fun updateCustomer(customer: Customer): Result<Customer>
    suspend fun getCustomerPurchases(storeId: String, customerId: String): Result<List<Sale>>
    suspend fun syncFromRemote(storeId: String): Result<Unit>
}
