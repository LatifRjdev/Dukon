package com.dokonpro.shared.data.remote

import com.dokonpro.shared.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ProductApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private fun authHeader(): String = "Bearer ${tokenProvider() ?: ""}"

    suspend fun getProducts(
        storeId: String,
        cursor: String? = null,
        search: String? = null
    ): ProductListResponse {
        return client.get("$baseUrl/stores/$storeId/products") {
            header("Authorization", authHeader())
            cursor?.let { parameter("cursor", it) }
            search?.let { parameter("search", it) }
        }.body()
    }

    suspend fun createProduct(storeId: String, request: CreateProductRequest): ProductDto =
        client.post("$baseUrl/stores/$storeId/products") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun updateProduct(
        storeId: String,
        productId: String,
        request: UpdateProductRequest
    ): ProductDto =
        client.patch("$baseUrl/stores/$storeId/products/$productId") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun deleteProduct(storeId: String, productId: String) {
        client.delete("$baseUrl/stores/$storeId/products/$productId") {
            header("Authorization", authHeader())
        }
    }

    suspend fun getCategories(storeId: String): List<CategoryDto> =
        client.get("$baseUrl/stores/$storeId/categories") {
            header("Authorization", authHeader())
        }.body()
}
