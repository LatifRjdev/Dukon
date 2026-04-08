package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Cart
import com.dokonpro.shared.domain.entity.PaymentMethod
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.repository.SaleRepository

class CompleteSaleUseCase(private val repository: SaleRepository) {
    suspend operator fun invoke(storeId: String, cart: Cart, discount: Double, paymentMethod: PaymentMethod, customerId: String? = null): Result<Sale> =
        repository.completeSale(storeId, cart, discount, paymentMethod, customerId)
}
