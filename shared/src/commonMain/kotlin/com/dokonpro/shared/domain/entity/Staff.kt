package com.dokonpro.shared.domain.entity

enum class Role { OWNER, MANAGER, CASHIER }

data class Staff(
    val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val role: Role,
    val storeId: String,
    val isActive: Boolean
)
