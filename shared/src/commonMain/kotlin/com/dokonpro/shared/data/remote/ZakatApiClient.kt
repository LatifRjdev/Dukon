package com.dokonpro.shared.data.remote

import com.dokonpro.shared.data.remote.dto.UpdateZakatConfigRequest
import com.dokonpro.shared.data.remote.dto.ZakatCalculationDto
import com.dokonpro.shared.data.remote.dto.ZakatConfigDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ZakatApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private fun auth(): String = "Bearer ${tokenProvider() ?: ""}"

    suspend fun calculate(storeId: String): ZakatCalculationDto =
        client.get("$baseUrl/stores/$storeId/zakat/calculate") {
            header("Authorization", auth())
        }.body()

    suspend fun save(storeId: String): ZakatCalculationDto =
        client.post("$baseUrl/stores/$storeId/zakat/save") {
            header("Authorization", auth())
        }.body()

    suspend fun getHistory(storeId: String): List<ZakatCalculationDto> =
        client.get("$baseUrl/stores/$storeId/zakat/history") {
            header("Authorization", auth())
        }.body()

    suspend fun getConfig(storeId: String): ZakatConfigDto =
        client.get("$baseUrl/stores/$storeId/zakat/config") {
            header("Authorization", auth())
        }.body()

    suspend fun updateConfig(storeId: String, request: UpdateZakatConfigRequest): ZakatConfigDto =
        client.patch("$baseUrl/stores/$storeId/zakat/config") {
            header("Authorization", auth())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
