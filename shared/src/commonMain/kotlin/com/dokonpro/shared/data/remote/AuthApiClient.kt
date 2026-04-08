package com.dokonpro.shared.data.remote

import com.dokonpro.shared.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApiClient(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun sendOtp(phone: String): SendOtpResponse =
        client.post("$baseUrl/auth/send-otp") {
            contentType(ContentType.Application.Json)
            setBody(SendOtpRequest(phone))
        }.body()

    suspend fun verifyOtp(phone: String, code: String): VerifyOtpResponse =
        client.post("$baseUrl/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(VerifyOtpRequest(phone, code))
        }.body()

    suspend fun register(phone: String, name: String, storeName: String): RegisterResponse =
        client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(phone, name, storeName))
        }.body()

    suspend fun refresh(refreshToken: String): RefreshResponse =
        client.post("$baseUrl/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }.body()
}
