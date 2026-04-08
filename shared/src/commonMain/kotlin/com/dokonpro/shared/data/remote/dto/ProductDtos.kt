package com.dokonpro.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val barcode: String? = null,
    val sku: String? = null,
    val price: Double,
    val costPrice: Double = 0.0,
    val quantity: Int = 0,
    val unit: String = "шт",
    val categoryId: String? = null,
    val category: CategoryDto? = null,
    val imageUrl: String? = null,
    val storeId: String,
    val isDeleted: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val storeId: String? = null
)

@Serializable
data class ProductListResponse(
    val products: List<ProductDto>,
    val nextCursor: String? = null
)

@Serializable
data class CreateProductRequest(
    val name: String,
    val barcode: String? = null,
    val sku: String? = null,
    val price: Double,
    val costPrice: Double = 0.0,
    val quantity: Int = 0,
    val unit: String = "шт",
    val categoryId: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val barcode: String? = null,
    val sku: String? = null,
    val price: Double? = null,
    val costPrice: Double? = null,
    val quantity: Int? = null,
    val unit: String? = null,
    val categoryId: String? = null,
    val imageUrl: String? = null
)
