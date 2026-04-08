package com.dokonpro.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dokonpro.shared.db.DokonProDatabase
import com.dokonpro.shared.domain.entity.Category
import com.dokonpro.shared.domain.entity.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class ProductLocalDataSource(private val db: DokonProDatabase) {

    fun getProducts(storeId: String): Flow<List<Product>> =
        db.productQueries.selectByStoreId(storeId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toProduct() } }

    fun searchProducts(storeId: String, query: String): Flow<List<Product>> =
        db.productQueries.searchByNameOrBarcode(storeId, query, query).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toProduct() } }

    fun getProductById(id: String): Product? =
        db.productQueries.selectById(id).executeAsOneOrNull()?.toProduct()

    fun insertProduct(product: Product) {
        db.productQueries.insertOrReplace(
            id = product.id,
            name = product.name,
            barcode = product.barcode,
            sku = product.sku,
            price = product.price,
            cost_price = product.costPrice,
            quantity = product.quantity.toLong(),
            unit = product.unit,
            category_id = product.categoryId,
            image_url = product.imageUrl,
            store_id = product.storeId,
            is_deleted = 0,
            created_at = product.createdAt,
            updated_at = product.updatedAt
        )
    }

    fun softDeleteProduct(id: String) {
        db.productQueries.softDelete(Clock.System.now().toString(), id)
    }

    fun insertProducts(products: List<Product>) {
        db.transaction { products.forEach { insertProduct(it) } }
    }

    fun getCategories(storeId: String): Flow<List<Category>> =
        db.categoryQueries.selectByStoreId(storeId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { Category(it.id, it.name, it.store_id) } }

    fun insertCategory(category: Category) {
        db.categoryQueries.insertOrReplace(
            category.id,
            category.name,
            category.storeId,
            Clock.System.now().toString()
        )
    }

    private fun com.dokonpro.shared.db.Products.toProduct() = Product(
        id = id,
        name = name,
        barcode = barcode,
        sku = sku,
        price = price,
        costPrice = cost_price,
        quantity = quantity.toInt(),
        unit = unit,
        categoryId = category_id,
        categoryName = null,
        imageUrl = image_url,
        storeId = store_id,
        createdAt = created_at,
        updatedAt = updated_at
    )
}
