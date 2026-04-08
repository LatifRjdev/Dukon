package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeCustomerRepository : CustomerRepository {
    val customers = mutableListOf<Customer>()
    override fun getCustomers(storeId: String): Flow<List<Customer>> = flowOf(customers.filter { it.storeId == storeId })
    override fun searchCustomers(storeId: String, query: String): Flow<List<Customer>> =
        flowOf(customers.filter { it.storeId == storeId && (it.name.contains(query, true) || it.phone?.contains(query) == true) })
    override suspend fun getCustomerById(id: String): Customer? = customers.find { it.id == id }
    override suspend fun createCustomer(storeId: String, customer: Customer): Result<Customer> { customers.add(customer); return Result.success(customer) }
    override suspend fun updateCustomer(customer: Customer): Result<Customer> {
        val idx = customers.indexOfFirst { it.id == customer.id }; if (idx >= 0) customers[idx] = customer; return Result.success(customer)
    }
    override suspend fun getCustomerPurchases(storeId: String, customerId: String): Result<List<Sale>> = Result.success(emptyList())
    override suspend fun syncFromRemote(storeId: String): Result<Unit> = Result.success(Unit)
}

private fun testCustomer(id: String = "c1", name: String = "Али Ахмедов") = Customer(
    id = id, name = name, phone = "+992901234567", email = null, notes = null,
    totalSpent = 1500.0, visitCount = 12, storeId = "store-1", createdAt = "2026-01-01", updatedAt = "2026-01-01"
)

class GetCustomersUseCaseTest {
    private val repo = FakeCustomerRepository()
    private val useCase = GetCustomersUseCase(repo)
    @Test fun `should return customers for store`() = runTest {
        repo.customers.add(testCustomer()); assertEquals(1, useCase("store-1").first().size)
    }
}

class CreateCustomerUseCaseTest {
    private val repo = FakeCustomerRepository()
    private val useCase = CreateCustomerUseCase(repo)
    @Test fun `should create customer`() = runTest {
        assertTrue(useCase("store-1", testCustomer()).isSuccess); assertEquals(1, repo.customers.size)
    }
}

class SearchCustomerUseCaseTest {
    private val repo = FakeCustomerRepository()
    private val useCase = SearchCustomerUseCase(repo)
    @Test fun `should find by name`() = runTest {
        repo.customers.add(testCustomer()); repo.customers.add(testCustomer(id = "c2", name = "Бобо"))
        assertEquals(1, useCase("store-1", "али").first().size)
    }
    @Test fun `should find by phone`() = runTest {
        repo.customers.add(testCustomer()); assertEquals(1, useCase("store-1", "901234").first().size)
    }
}
