package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class SearchProductUseCase(private val repository: ProductRepository) {
    operator fun invoke(storeId: String, query: String): Flow<List<Product>> =
        repository.searchProducts(storeId, query)
}
