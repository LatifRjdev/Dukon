package com.dokonpro.shared.domain.entity

data class FinanceSummary(
    val revenue: Double,
    val expenses: Double,
    val profit: Double,
    val period: String,
    val dateFrom: String,
    val dateTo: String
)

data class DailyRevenue(
    val date: String,
    val amount: Double
)
