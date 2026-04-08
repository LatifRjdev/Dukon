package com.dokonpro.shared.data.repository

import com.dokonpro.shared.data.local.TransactionLocalDataSource
import com.dokonpro.shared.data.remote.FinanceApiClient
import com.dokonpro.shared.data.remote.dto.AddExpenseRequest
import com.dokonpro.shared.data.sync.SyncQueue
import com.dokonpro.shared.domain.entity.*
import com.dokonpro.shared.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FinanceRepositoryImpl(
    private val local: TransactionLocalDataSource,
    private val api: FinanceApiClient,
    private val syncQueue: SyncQueue
) : FinanceRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getSummary(storeId: String, period: String): Result<FinanceSummary> = runCatching {
        val dto = api.getSummary(storeId, period)
        FinanceSummary(dto.revenue, dto.expenses, dto.profit, dto.period, dto.dateFrom, dto.dateTo)
    }

    override fun getTransactions(storeId: String): Flow<List<Transaction>> = local.getTransactions(storeId)

    override fun getTransactionsByType(storeId: String, type: TransactionType): Flow<List<Transaction>> =
        local.getTransactionsByType(storeId, type.name)

    override suspend fun addExpense(storeId: String, amount: Double, description: String?, categoryId: String?): Result<Transaction> = runCatching {
        val now = Clock.System.now().toString()
        val id = "tx-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}"
        val tx = Transaction(id, TransactionType.EXPENSE, amount, description, categoryId, null, storeId, now)
        local.insertTransaction(tx)
        val request = AddExpenseRequest(amount, description, categoryId)
        syncQueue.enqueue("finance/expenses", "$storeId:$id", "CREATE", json.encodeToString(request))
        tx
    }

    override suspend fun getReport(storeId: String, period: String): Result<List<DailyRevenue>> = runCatching {
        api.getReport(storeId, period).map { DailyRevenue(it.date, it.amount) }
    }

    override fun getExpenseCategories(storeId: String): Flow<List<ExpenseCategory>> = local.getCategories(storeId)

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = runCatching {
        val txs = api.getTransactions(storeId)
        local.insertTransactions(txs.map { dto ->
            Transaction(dto.id, TransactionType.valueOf(dto.type), dto.amount, dto.description,
                dto.categoryId, dto.category?.name, dto.storeId, dto.createdAt)
        })
        val cats = api.getCategories(storeId)
        cats.forEach { local.insertCategory(ExpenseCategory(it.id, it.name, it.storeId ?: storeId)) }
    }
}
