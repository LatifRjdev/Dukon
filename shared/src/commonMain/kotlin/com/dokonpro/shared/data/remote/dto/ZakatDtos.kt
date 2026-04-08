package com.dokonpro.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ZakatCalculationDto(
    val id: String? = null,
    val inventoryValue: Double,
    val cashBalance: Double,
    val receivables: Double = 0.0,
    val liabilities: Double = 0.0,
    val nisabThreshold: Double,
    val zakatableAmount: Double,
    val zakatDue: Double,
    val goldRate: Double,
    val storeId: String,
    val calculatedAt: String? = null
)

@Serializable
data class ZakatConfigDto(
    val id: String? = null,
    val storeId: String? = null,
    val goldRatePerGram: Double,
    val silverRatePerGram: Double
)

@Serializable
data class UpdateZakatConfigRequest(
    val goldRatePerGram: Double? = null,
    val silverRatePerGram: Double? = null
)
