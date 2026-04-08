package com.dokonpro.shared.domain.entity

data class CartItem(
    val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val discount: Double = 0.0
) {
    val subtotal: Double get() = (price - discount) * quantity
}

data class Cart(
    val items: List<CartItem> = emptyList()
) {
    val totalItems: Int get() = items.sumOf { it.quantity }
    val subtotal: Double get() = items.sumOf { it.subtotal }
    fun totalWithDiscount(saleDiscount: Double): Double =
        (subtotal - saleDiscount).coerceAtLeast(0.0)
}
