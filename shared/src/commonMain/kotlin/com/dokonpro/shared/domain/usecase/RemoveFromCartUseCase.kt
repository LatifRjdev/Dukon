package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Cart

class RemoveFromCartUseCase {
    operator fun invoke(cart: Cart, productId: String): Cart =
        cart.copy(items = cart.items.filter { it.productId != productId })
}
