package com.dokonpro.shared.data.remote

import com.dokonpro.shared.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class StaffApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private fun authHeader(): String = "Bearer ${tokenProvider() ?: ""}"

    suspend fun getStaff(storeId: String): List<StaffDto> =
        client.get("$baseUrl/stores/$storeId/staff") { header("Authorization", authHeader()) }.body()

    suspend fun addStaff(storeId: String, request: AddStaffRequest): StaffDto =
        client.post("$baseUrl/stores/$storeId/staff") {
            header("Authorization", authHeader()); contentType(ContentType.Application.Json); setBody(request)
        }.body()

    suspend fun updateStaff(storeId: String, staffId: String, request: UpdateStaffRequest): StaffDto =
        client.patch("$baseUrl/stores/$storeId/staff/$staffId") {
            header("Authorization", authHeader()); contentType(ContentType.Application.Json); setBody(request)
        }.body()

    suspend fun deactivateStaff(storeId: String, staffId: String) {
        client.delete("$baseUrl/stores/$storeId/staff/$staffId") { header("Authorization", authHeader()) }
    }
}
