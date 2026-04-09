package com.dokonpro.shared.data.local

import com.dokonpro.shared.domain.entity.AuthTokens
import platform.Foundation.NSUserDefaults

actual class TokenStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun saveTokens(tokens: AuthTokens) {
        defaults.setObject(tokens.accessToken, "access_token")
        defaults.setObject(tokens.refreshToken, "refresh_token")
    }

    actual fun getTokens(): AuthTokens? {
        val access = defaults.stringForKey("access_token") ?: return null
        val refresh = defaults.stringForKey("refresh_token") ?: return null
        return AuthTokens(accessToken = access, refreshToken = refresh)
    }

    actual fun clearTokens() {
        defaults.removeObjectForKey("access_token")
        defaults.removeObjectForKey("refresh_token")
    }

    actual fun hasTokens(): Boolean = defaults.stringForKey("access_token") != null

    actual fun saveStoreId(storeId: String) {
        defaults.setObject(storeId, "store_id")
    }

    actual fun getStoreId(): String? = defaults.stringForKey("store_id")

    actual fun clearStoreId() {
        defaults.removeObjectForKey("store_id")
    }
}
