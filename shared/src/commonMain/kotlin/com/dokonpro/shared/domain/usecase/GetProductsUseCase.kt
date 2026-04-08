package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class GetProductsUseCase(private val repository: ProductRepository) {
    operator fun invoke(storeId: String): Flow<List<Product>> =
        repository.getProducts(storeId)
}
