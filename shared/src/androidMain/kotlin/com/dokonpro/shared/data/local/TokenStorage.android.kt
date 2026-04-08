package com.dokonpro.shared.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dokonpro.shared.domain.entity.AuthTokens

actual class TokenStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "dokonpro_auth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    actual fun saveTokens(tokens: AuthTokens) {
        prefs.edit()
            .putString("access_token", tokens.accessToken)
            .putString("refresh_token", tokens.refreshToken)
            .apply()
    }

    actual fun getTokens(): AuthTokens? {
        val access = prefs.getString("access_token", null) ?: return null
        val refresh = prefs.getString("refresh_token", null) ?: return null
        return AuthTokens(accessToken = access, refreshToken = refresh)
    }

    actual fun clearTokens() {
        prefs.edit().clear().apply()
    }

    actual fun hasTokens(): Boolean = prefs.getString("access_token", null) != null
}
