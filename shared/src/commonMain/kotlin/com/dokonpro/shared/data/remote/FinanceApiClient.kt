package com.dokonpro.shared.data.remote

import com.dokonpro.shared.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class FinanceApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private fun authHeader(): String = "Bearer ${tokenProvider() ?: ""}"

    suspend fun getSummary(storeId: String, period: String): FinanceSummaryDto =
        client.get("$baseUrl/stores/$storeId/finance/summary") {
            header("Authorization", authHeader()); parameter("period", period)
        }.body()

    suspend fun getTransactions(storeId: String, type: String? = null): List<TransactionDto> =
        client.get("$baseUrl/stores/$storeId/finance/transactions") {
            header("Authorization", authHeader()); type?.let { parameter("type", it) }
        }.body()

    suspend fun addExpense(storeId: String, request: AddExpenseRequest): TransactionDto =
        client.post("$baseUrl/stores/$storeId/finance/expenses") {
            header("Authorization", authHeader()); contentType(ContentType.Application.Json); setBody(request)
        }.body()

    suspend fun getReport(storeId: String, period: String): List<DailyRevenueDto> =
        client.get("$baseUrl/stores/$storeId/finance/reports") {
            header("Authorization", authHeader()); parameter("period", period)
        }.body()

    suspend fun getCategories(storeId: String): List<ExpenseCategoryDto> =
        client.get("$baseUrl/stores/$storeId/finance/categories") {
            header("Authorization", authHeader())
        }.body()
}
