package com.dokonpro.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequest(val phone: String)

@Serializable
data class SendOtpResponse(val message: String)

@Serializable
data class VerifyOtpRequest(val phone: String, val code: String)

@Serializable
data class VerifyOtpResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean
)

@Serializable
data class RegisterRequest(val phone: String, val name: String, val storeName: String)

@Serializable
data class RegisterResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto,
    val store: StoreDto
)

@Serializable
data class UserDto(val id: String, val phone: String, val name: String)

@Serializable
data class StoreDto(val id: String, val name: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class RefreshResponse(val accessToken: String, val refreshToken: String)
