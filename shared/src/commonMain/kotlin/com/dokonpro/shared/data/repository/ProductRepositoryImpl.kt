package com.dokonpro.shared.data.repository

import com.dokonpro.shared.data.local.ProductLocalDataSource
import com.dokonpro.shared.data.remote.ProductApiClient
import com.dokonpro.shared.data.remote.dto.CreateProductRequest
import com.dokonpro.shared.data.remote.dto.UpdateProductRequest
import com.dokonpro.shared.data.sync.SyncQueue
import com.dokonpro.shared.domain.entity.Category
import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProductRepositoryImpl(
    private val local: ProductLocalDataSource,
    private val api: ProductApiClient,
    private val syncQueue: SyncQueue
) : ProductRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getProducts(storeId: String): Flow<List<Product>> =
        local.getProducts(storeId)

    override fun searchProducts(storeId: String, query: String): Flow<List<Product>> =
        local.searchProducts(storeId, query)

    override suspend fun getProductById(id: String): Product? =
        local.getProductById(id)

    override suspend fun createProduct(storeId: String, product: Product): Result<Product> = runCatching {
        local.insertProduct(product)
        val request = CreateProductRequest(
            name = product.name,
            barcode = product.barcode,
            sku = product.sku,
            price = product.price,
            costPrice = product.costPrice,
            quantity = product.quantity,
            unit = product.unit,
            categoryId = product.categoryId,
            imageUrl = product.imageUrl
        )
        syncQueue.enqueue(
            "products",
            "$storeId:${product.id}",
            "CREATE",
            json.encodeToString(request)
        )
        product
    }

    override suspend fun updateProduct(product: Product): Result<Product> = runCatching {
        local.insertProduct(product)
        val request = UpdateProductRequest(
            name = product.name,
            barcode = product.barcode,
            sku = product.sku,
            price = product.price,
            costPrice = product.costPrice,
            quantity = product.quantity,
            unit = product.unit,
            categoryId = product.categoryId,
            imageUrl = product.imageUrl
        )
        syncQueue.enqueue(
            "products",
            "${product.storeId}:${product.id}",
            "UPDATE",
            json.encodeToString(request)
        )
        product
    }

    override suspend fun deleteProduct(storeId: String, productId: String): Result<Unit> = runCatching {
        local.softDeleteProduct(productId)
        syncQueue.enqueue("products", "$storeId:$productId", "DELETE", "{}")
    }

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = runCatching {
        val response = api.getProducts(storeId)
        val products = response.products.map { dto ->
            Product(
                id = dto.id,
                name = dto.name,
                barcode = dto.barcode,
                sku = dto.sku,
                price = dto.price,
                costPrice = dto.costPrice,
                quantity = dto.quantity,
                unit = dto.unit,
                categoryId = dto.categoryId,
                categoryName = dto.category?.name,
                imageUrl = dto.imageUrl,
                storeId = dto.storeId,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt
            )
        }
        local.insertProducts(products)

        val categories = api.getCategories(storeId)
        categories.forEach {
            local.insertCategory(Category(it.id, it.name, it.storeId ?: storeId))
        }
    }

    override fun getCategories(storeId: String): Flow<List<Category>> =
        local.getCategories(storeId)
}
