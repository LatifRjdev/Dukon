package com.dokonpro.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CustomerDto(
    val id: String, val name: String, val phone: String? = null, val email: String? = null,
    val notes: String? = null, val totalSpent: Double = 0.0, val visitCount: Int = 0,
    val storeId: String, val createdAt: String, val updatedAt: String
)

@Serializable
data class CustomerListResponse(val customers: List<CustomerDto>, val nextCursor: String? = null)

@Serializable
data class CreateCustomerRequest(val name: String, val phone: String? = null, val email: String? = null, val notes: String? = null)

@Serializable
data class UpdateCustomerRequest(val name: String? = null, val phone: String? = null, val email: String? = null, val notes: String? = null)
