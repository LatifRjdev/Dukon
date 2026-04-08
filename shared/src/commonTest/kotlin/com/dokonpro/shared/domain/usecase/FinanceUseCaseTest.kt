package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.*
import com.dokonpro.shared.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeFinanceRepository : FinanceRepository {
    var summaryResult: Result<FinanceSummary> = Result.success(
        FinanceSummary(revenue = 5000.0, expenses = 2000.0, profit = 3000.0, period = "day", dateFrom = "2026-04-08", dateTo = "2026-04-08")
    )
    val transactions = mutableListOf<Transaction>()
    var addExpenseResult: Result<Transaction> = Result.success(
        Transaction("t1", TransactionType.EXPENSE, 500.0, "Electricity", null, null, "store-1", "2026-04-08")
    )
    var reportResult: Result<List<DailyRevenue>> = Result.success(
        listOf(DailyRevenue("2026-04-07", 3000.0), DailyRevenue("2026-04-08", 5000.0))
    )

    override suspend fun getSummary(storeId: String, period: String): Result<FinanceSummary> = summaryResult
    override fun getTransactions(storeId: String): Flow<List<Transaction>> = flowOf(transactions)
    override fun getTransactionsByType(storeId: String, type: TransactionType): Flow<List<Transaction>> =
        flowOf(transactions.filter { it.type == type })
    override suspend fun addExpense(storeId: String, amount: Double, description: String?, categoryId: String?): Result<Transaction> = addExpenseResult
    override suspend fun getReport(storeId: String, period: String): Result<List<DailyRevenue>> = reportResult
    override fun getExpenseCategories(storeId: String): Flow<List<ExpenseCategory>> = flowOf(emptyList())
    override suspend fun syncFromRemote(storeId: String): Result<Unit> = Result.success(Unit)
}

class GetFinanceSummaryUseCaseTest {
    private val repo = FakeFinanceRepository()
    private val useCase = GetFinanceSummaryUseCase(repo)

    @Test
    fun `should return summary with profit`() = runTest {
        val result = useCase("store-1", "day")
        assertTrue(result.isSuccess)
        assertEquals(3000.0, result.getOrThrow().profit)
    }
}

class AddExpenseUseCaseTest {
    private val repo = FakeFinanceRepository()
    private val useCase = AddExpenseUseCase(repo)

    @Test
    fun `should add expense`() = runTest {
        val result = useCase("store-1", 500.0, "Electricity", null)
        assertTrue(result.isSuccess)
        assertEquals(500.0, result.getOrThrow().amount)
    }
}

class GetReportUseCaseTest {
    private val repo = FakeFinanceRepository()
    private val useCase = GetReportUseCase(repo)

    @Test
    fun `should return daily revenue report`() = runTest {
        val result = useCase("store-1", "week")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
    }
}
