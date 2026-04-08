package com.dokonpro.shared.domain.entity

data class ZakatCalculation(
    val id: String,
    val inventoryValue: Double,
    val cashBalance: Double,
    val receivables: Double,
    val liabilities: Double,
    val nisabThreshold: Double,
    val zakatableAmount: Double,
    val zakatDue: Double,
    val goldRate: Double,
    val storeId: String,
    val calculatedAt: String
)

data class ZakatConfig(
    val goldRatePerGram: Double,
    val silverRatePerGram: Double
)
