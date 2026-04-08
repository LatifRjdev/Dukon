package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Category
import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeProductRepository : ProductRepository {
    val products = mutableListOf<Product>()
    var deleteResult: Result<Unit> = Result.success(Unit)

    override fun getProducts(storeId: String): Flow<List<Product>> =
        flowOf(products.filter { it.storeId == storeId })

    override fun searchProducts(storeId: String, query: String): Flow<List<Product>> =
        flowOf(products.filter {
            it.storeId == storeId &&
                (it.name.contains(query, true) || it.barcode?.contains(query) == true)
        })

    override suspend fun getProductById(id: String): Product? =
        products.find { it.id == id }

    override suspend fun createProduct(storeId: String, product: Product): Result<Product> {
        products.add(product)
        return Result.success(product)
    }

    override suspend fun updateProduct(product: Product): Result<Product> {
        val idx = products.indexOfFirst { it.id == product.id }
        if (idx >= 0) products[idx] = product
        return Result.success(product)
    }

    override suspend fun deleteProduct(storeId: String, productId: String): Result<Unit> {
        products.removeAll { it.id == productId }
        return deleteResult
    }

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = Result.success(Unit)

    override fun getCategories(storeId: String): Flow<List<Category>> = flowOf(emptyList())
}

private fun testProduct(
    id: String = "p1",
    name: String = "Coca-Cola",
    storeId: String = "store-1"
) = Product(
    id = id,
    name = name,
    barcode = "123456",
    sku = null,
    price = 12.5,
    costPrice = 8.0,
    quantity = 48,
    unit = "шт",
    categoryId = null,
    categoryName = null,
    imageUrl = null,
    storeId = storeId,
    createdAt = "2026-01-01",
    updatedAt = "2026-01-01"
)

class GetProductsUseCaseTest {
    private val repo = FakeProductRepository()
    private val useCase = GetProductsUseCase(repo)

    @Test
    fun `should return products for store`() = runTest {
        repo.products.add(testProduct())
        val result = useCase("store-1").first()
        assertEquals(1, result.size)
        assertEquals("Coca-Cola", result[0].name)
    }
}

class CreateProductUseCaseTest {
    private val repo = FakeProductRepository()
    private val useCase = CreateProductUseCase(repo)

    @Test
    fun `should create product and return it`() = runTest {
        val result = useCase("store-1", testProduct())
        assertTrue(result.isSuccess)
        assertEquals(1, repo.products.size)
    }
}

class SearchProductUseCaseTest {
    private val repo = FakeProductRepository()
    private val useCase = SearchProductUseCase(repo)

    @Test
    fun `should find product by name`() = runTest {
        repo.products.add(testProduct())
        repo.products.add(testProduct(id = "p2", name = "Fanta"))
        val result = useCase("store-1", "coca").first()
        assertEquals(1, result.size)
        assertEquals("Coca-Cola", result[0].name)
    }
}

class DeleteProductUseCaseTest {
    private val repo = FakeProductRepository()
    private val useCase = DeleteProductUseCase(repo)

    @Test
    fun `should delete product`() = runTest {
        repo.products.add(testProduct())
        val result = useCase("store-1", "p1")
        assertTrue(result.isSuccess)
        assertEquals(0, repo.products.size)
    }
}
