package com.dokonpro.shared.data.remote

import com.dokonpro.shared.data.remote.dto.StoreSettingsDto
import com.dokonpro.shared.data.remote.dto.UpdateStoreSettingsRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class StoreSettingsApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private fun auth(): String = "Bearer ${tokenProvider() ?: ""}"

    suspend fun getSettings(storeId: String): StoreSettingsDto =
        client.get("$baseUrl/stores/$storeId") {
            header("Authorization", auth())
        }.body()

    suspend fun updateSettings(storeId: String, request: UpdateStoreSettingsRequest): StoreSettingsDto =
        client.patch("$baseUrl/stores/$storeId") {
            header("Authorization", auth())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
