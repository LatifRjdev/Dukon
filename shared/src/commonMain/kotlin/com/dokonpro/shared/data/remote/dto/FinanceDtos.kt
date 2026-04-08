package com.dokonpro.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FinanceSummaryDto(
    val revenue: Double, val expenses: Double, val profit: Double,
    val period: String, val dateFrom: String, val dateTo: String
)

@Serializable
data class TransactionDto(
    val id: String, val type: String, val amount: Double, val description: String? = null,
    val categoryId: String? = null, val category: ExpenseCategoryDto? = null,
    val storeId: String, val createdAt: String
)

@Serializable
data class ExpenseCategoryDto(val id: String, val name: String, val storeId: String? = null)

@Serializable
data class AddExpenseRequest(val amount: Double, val description: String? = null, val categoryId: String? = null)

@Serializable
data class DailyRevenueDto(val date: String, val amount: Double)
