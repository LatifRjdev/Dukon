package com.dokonpro.shared.data.local

class SessionProvider(private val tokenStorage: TokenStorage) {
    val storeId: String
        get() = tokenStorage.getStoreId() ?: "no-store"

    val isLoggedIn: Boolean
        get() = tokenStorage.hasTokens() && tokenStorage.getStoreId() != null
}
