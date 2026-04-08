package com.dokonpro.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StaffDto(
    val id: String, val userId: String, val name: String?,
    val phone: String, val role: String, val storeId: String
)

@Serializable
data class AddStaffRequest(val phone: String, val name: String, val role: String)

@Serializable
data class UpdateStaffRequest(val role: String? = null, val isActive: Boolean? = null)
