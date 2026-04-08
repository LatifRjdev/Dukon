package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Cart
import com.dokonpro.shared.domain.entity.CartItem

class AddToCartUseCase {
    operator fun invoke(cart: Cart, productId: String, name: String, price: Double): Cart {
        val existing = cart.items.find { it.productId == productId }
        val updatedItems = if (existing != null) {
            cart.items.map { if (it.productId == productId) it.copy(quantity = it.quantity + 1) else it }
        } else {
            cart.items + CartItem(productId = productId, name = name, price = price, quantity = 1)
        }
        return cart.copy(items = updatedItems)
    }
}
