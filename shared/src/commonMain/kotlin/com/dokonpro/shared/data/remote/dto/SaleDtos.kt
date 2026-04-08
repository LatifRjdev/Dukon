package com.dokonpro.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateSaleItemRequest(val productId: String, val name: String, val quantity: Int, val price: Double, val discount: Double = 0.0)

@Serializable
data class CreateSaleRequest(val items: List<CreateSaleItemRequest>, val totalAmount: Double, val discount: Double = 0.0, val paymentMethod: String = "CASH", val customerId: String? = null)

@Serializable
data class SaleDto(val id: String, val totalAmount: Double, val discount: Double = 0.0, val paymentMethod: String = "CASH", val customerId: String? = null, val storeId: String, val isRefunded: Boolean = false, val createdAt: String, val items: List<SaleItemDto> = emptyList())

@Serializable
data class SaleItemDto(val id: String, val saleId: String, val productId: String, val name: String, val quantity: Int, val price: Double, val discount: Double = 0.0)

@Serializable
data class SaleListResponse(val sales: List<SaleDto>, val nextCursor: String? = null)
