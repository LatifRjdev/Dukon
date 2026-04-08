package com.dokonpro.shared.data.local

import com.dokonpro.shared.domain.entity.AuthTokens

expect class TokenStorage {
    fun saveTokens(tokens: AuthTokens)
    fun getTokens(): AuthTokens?
    fun clearTokens()
    fun hasTokens(): Boolean
}
