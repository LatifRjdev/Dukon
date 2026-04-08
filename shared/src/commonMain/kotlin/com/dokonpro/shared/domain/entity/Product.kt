package com.dokonpro.shared.domain.entity

data class Product(
    val id: String,
    val name: String,
    val barcode: String?,
    val sku: String?,
    val price: Double,
    val costPrice: Double,
    val quantity: Int,
    val unit: String,
    val categoryId: String?,
    val categoryName: String?,
    val imageUrl: String?,
    val storeId: String,
    val createdAt: String,
    val updatedAt: String
)
