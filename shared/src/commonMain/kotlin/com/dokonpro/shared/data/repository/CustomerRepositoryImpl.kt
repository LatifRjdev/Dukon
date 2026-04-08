package com.dokonpro.shared.data.repository

import com.dokonpro.shared.data.local.CustomerLocalDataSource
import com.dokonpro.shared.data.remote.CustomerApiClient
import com.dokonpro.shared.data.remote.dto.CreateCustomerRequest
import com.dokonpro.shared.data.remote.dto.UpdateCustomerRequest
import com.dokonpro.shared.data.sync.SyncQueue
import com.dokonpro.shared.domain.entity.*
import com.dokonpro.shared.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CustomerRepositoryImpl(
    private val local: CustomerLocalDataSource,
    private val api: CustomerApiClient,
    private val syncQueue: SyncQueue
) : CustomerRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override fun getCustomers(storeId: String): Flow<List<Customer>> = local.getCustomers(storeId)
    override fun searchCustomers(storeId: String, query: String): Flow<List<Customer>> = local.searchCustomers(storeId, query)
    override suspend fun getCustomerById(id: String): Customer? = local.getCustomerById(id)

    override suspend fun createCustomer(storeId: String, customer: Customer): Result<Customer> = runCatching {
        local.insertCustomer(customer)
        syncQueue.enqueue("customers", "$storeId:${customer.id}", "CREATE",
            json.encodeToString(CreateCustomerRequest(customer.name, customer.phone, customer.email, customer.notes)))
        customer
    }

    override suspend fun updateCustomer(customer: Customer): Result<Customer> = runCatching {
        local.insertCustomer(customer)
        syncQueue.enqueue("customers", "${customer.storeId}:${customer.id}", "UPDATE",
            json.encodeToString(UpdateCustomerRequest(customer.name, customer.phone, customer.email, customer.notes)))
        customer
    }

    override suspend fun getCustomerPurchases(storeId: String, customerId: String): Result<List<Sale>> = runCatching {
        api.getPurchases(storeId, customerId).map { dto ->
            Sale(dto.id, dto.totalAmount, dto.discount, PaymentMethod.valueOf(dto.paymentMethod),
                dto.customerId, dto.storeId, dto.isRefunded, dto.createdAt,
                dto.items.map { SaleItem(it.id, it.saleId, it.productId, it.name, it.quantity, it.price, it.discount) })
        }
    }

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = runCatching {
        val response = api.getCustomers(storeId)
        local.insertCustomers(response.customers.map { dto ->
            Customer(dto.id, dto.name, dto.phone, dto.email, dto.notes, dto.totalSpent, dto.visitCount, dto.storeId, dto.createdAt, dto.updatedAt)
        })
    }
}
