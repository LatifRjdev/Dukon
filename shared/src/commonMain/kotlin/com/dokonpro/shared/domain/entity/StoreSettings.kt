package com.dokonpro.shared.domain.entity

data class StoreSettings(
    val id: String,
    val name: String,
    val address: String?,
    val phone: String?,
    val currency: String,
    val logoUrl: String?,
    val receiptHeader: String?,
    val receiptFooter: String?,
    val storeId: String
)
