package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.repository.ProductRepository

class DeleteProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(storeId: String, productId: String): Result<Unit> =
        repository.deleteProduct(storeId, productId)
}
