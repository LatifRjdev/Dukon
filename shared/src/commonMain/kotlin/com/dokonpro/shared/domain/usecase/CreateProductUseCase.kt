package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository

class CreateProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(storeId: String, product: Product): Result<Product> =
        repository.createProduct(storeId, product)
}
