package com.dokonpro.shared.domain.entity

enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val description: String?,
    val categoryId: String?,
    val categoryName: String?,
    val storeId: String,
    val createdAt: String
)

data class ExpenseCategory(
    val id: String,
    val name: String,
    val storeId: String
)
