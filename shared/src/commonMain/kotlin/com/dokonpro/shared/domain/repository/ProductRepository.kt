package com.dokonpro.shared.domain.repository

import com.dokonpro.shared.domain.entity.Category
import com.dokonpro.shared.domain.entity.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(storeId: String): Flow<List<Product>>
    fun searchProducts(storeId: String, query: String): Flow<List<Product>>
    suspend fun getProductById(id: String): Product?
    suspend fun createProduct(storeId: String, product: Product): Result<Product>
    suspend fun updateProduct(product: Product): Result<Product>
    suspend fun deleteProduct(storeId: String, productId: String): Result<Unit>
    suspend fun syncFromRemote(storeId: String): Result<Unit>
    fun getCategories(storeId: String): Flow<List<Category>>
}
