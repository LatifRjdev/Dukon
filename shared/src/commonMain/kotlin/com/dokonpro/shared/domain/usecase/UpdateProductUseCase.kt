package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository

class UpdateProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(product: Product): Result<Product> =
        repository.updateProduct(product)
}
