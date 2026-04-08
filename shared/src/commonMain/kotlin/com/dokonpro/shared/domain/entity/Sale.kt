package com.dokonpro.shared.domain.entity

enum class PaymentMethod { CASH, CARD, MIXED }

data class Sale(
    val id: String,
    val totalAmount: Double,
    val discount: Double,
    val paymentMethod: PaymentMethod,
    val customerId: String?,
    val storeId: String,
    val isRefunded: Boolean,
    val createdAt: String,
    val items: List<SaleItem>
)

data class SaleItem(
    val id: String,
    val saleId: String,
    val productId: String,
    val name: String,
    val quantity: Int,
    val price: Double,
    val discount: Double
)
