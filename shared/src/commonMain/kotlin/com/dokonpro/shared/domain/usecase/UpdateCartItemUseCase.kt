package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Cart

class UpdateCartItemUseCase {
    operator fun invoke(cart: Cart, productId: String, quantity: Int, discount: Double = 0.0): Cart {
        if (quantity <= 0) return cart.copy(items = cart.items.filter { it.productId != productId })
        return cart.copy(items = cart.items.map {
            if (it.productId == productId) it.copy(quantity = quantity, discount = discount) else it
        })
    }
}
