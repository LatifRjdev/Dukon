package com.dokonpro.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dokonpro.shared.db.DokonProDatabase
import com.dokonpro.shared.domain.entity.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerLocalDataSource(private val db: DokonProDatabase) {
    fun getCustomers(storeId: String): Flow<List<Customer>> =
        db.customerQueries.selectByStoreId(storeId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toCustomer() } }

    fun searchCustomers(storeId: String, query: String): Flow<List<Customer>> =
        db.customerQueries.searchByNameOrPhone(storeId, query, query).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toCustomer() } }

    fun getCustomerById(id: String): Customer? = db.customerQueries.selectById(id).executeAsOneOrNull()?.toCustomer()

    fun insertCustomer(customer: Customer) {
        db.customerQueries.insertOrReplace(customer.id, customer.name, customer.phone, customer.email,
            customer.notes, customer.totalSpent, customer.visitCount.toLong(), customer.storeId,
            customer.createdAt, customer.updatedAt)
    }

    fun insertCustomers(customers: List<Customer>) { db.transaction { customers.forEach { insertCustomer(it) } } }

    private fun com.dokonpro.shared.db.Customers.toCustomer() = Customer(
        id, name, phone, email, notes, total_spent, visit_count.toInt(), store_id, created_at, updated_at)
}
