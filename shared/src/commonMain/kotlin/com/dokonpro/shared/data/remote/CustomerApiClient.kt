package com.dokonpro.shared.data.remote

import com.dokonpro.shared.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class CustomerApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private fun authHeader(): String = "Bearer ${tokenProvider() ?: ""}"

    suspend fun getCustomers(storeId: String, search: String? = null): CustomerListResponse =
        client.get("$baseUrl/stores/$storeId/customers") {
            header("Authorization", authHeader()); search?.let { parameter("search", it) }
        }.body()

    suspend fun createCustomer(storeId: String, request: CreateCustomerRequest): CustomerDto =
        client.post("$baseUrl/stores/$storeId/customers") {
            header("Authorization", authHeader()); contentType(ContentType.Application.Json); setBody(request)
        }.body()

    suspend fun updateCustomer(storeId: String, customerId: String, request: UpdateCustomerRequest): CustomerDto =
        client.patch("$baseUrl/stores/$storeId/customers/$customerId") {
            header("Authorization", authHeader()); contentType(ContentType.Application.Json); setBody(request)
        }.body()

    suspend fun getPurchases(storeId: String, customerId: String): List<SaleDto> =
        client.get("$baseUrl/stores/$storeId/customers/$customerId/purchases") {
            header("Authorization", authHeader())
        }.body()
}
