package com.dokonpro.shared.domain.repository

import com.dokonpro.shared.domain.entity.*
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    suspend fun getSummary(storeId: String, period: String): Result<FinanceSummary>
    fun getTransactions(storeId: String): Flow<List<Transaction>>
    fun getTransactionsByType(storeId: String, type: TransactionType): Flow<List<Transaction>>
    suspend fun addExpense(storeId: String, amount: Double, description: String?, categoryId: String?): Result<Transaction>
    suspend fun getReport(storeId: String, period: String): Result<List<DailyRevenue>>
    fun getExpenseCategories(storeId: String): Flow<List<ExpenseCategory>>
    suspend fun syncFromRemote(storeId: String): Result<Unit>
}
