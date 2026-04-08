package com.dokonpro.shared.domain.entity

data class Customer(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val notes: String?,
    val totalSpent: Double,
    val visitCount: Int,
    val storeId: String,
    val createdAt: String,
    val updatedAt: String
)
