package com.dokonpro.shared.domain.entity

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)
