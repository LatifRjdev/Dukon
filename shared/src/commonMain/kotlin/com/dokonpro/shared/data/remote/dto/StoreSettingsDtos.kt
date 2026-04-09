package com.dokonpro.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StoreSettingsDto(
    val id: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val currency: String = "TJS",
    val logoUrl: String? = null,
    val receiptHeader: String? = null,
    val receiptFooter: String? = null
)

@Serializable
data class UpdateStoreSettingsRequest(
    val name: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val currency: String? = null,
    val logoUrl: String? = null,
    val receiptHeader: String? = null,
    val receiptFooter: String? = null
)
