package com.dokonpro.shared.data.remote

import com.dokonpro.shared.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SaleApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private fun authHeader(): String = "Bearer ${tokenProvider() ?: ""}"

    suspend fun createSale(storeId: String, request: CreateSaleRequest): SaleDto =
        client.post("$baseUrl/stores/$storeId/sales") {
            header("Authorization", authHeader()); contentType(ContentType.Application.Json); setBody(request)
        }.body()

    suspend fun getSales(storeId: String, cursor: String? = null): SaleListResponse =
        client.get("$baseUrl/stores/$storeId/sales") {
            header("Authorization", authHeader()); cursor?.let { parameter("cursor", it) }
        }.body()

    suspend fun getSaleById(storeId: String, saleId: String): SaleDto =
        client.get("$baseUrl/stores/$storeId/sales/$saleId") { header("Authorization", authHeader()) }.body()

    suspend fun refundSale(storeId: String, saleId: String): SaleDto =
        client.post("$baseUrl/stores/$storeId/sales/$saleId/refund") { header("Authorization", authHeader()) }.body()
}
