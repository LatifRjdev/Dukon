package com.dokonpro.shared.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.min
import kotlin.math.pow

class SyncManager(
    private val queue: SyncQueue,
    private val client: HttpClient,
    private val baseUrl: String,
    private val syncStatus: SyncStatus,
    private val tokenProvider: () -> String?
) {
    private var syncJob: Job? = null

    val status: StateFlow<SyncStatusData> = syncStatus.status

    fun startSync(scope: CoroutineScope) {
        syncJob?.cancel()
        syncJob = scope.launch {
            processQueue()
        }
    }

    suspend fun processQueue() {
        val pending = queue.getPending()
        if (pending.isEmpty()) {
            syncStatus.update(SyncState.IDLE, 0)
            return
        }

        syncStatus.update(SyncState.SYNCING, pending.size.toLong())

        for (entry in pending) {
            try {
                executeSync(entry)
                queue.markCompleted(entry.id)
            } catch (e: Exception) {
                val newRetry = entry.retryCount + 1
                queue.markFailed(entry.id, newRetry)
                if (newRetry < 5) {
                    val delayMs = (1000.0 * 2.0.pow(newRetry.toDouble())).toLong()
                    delay(min(delayMs, 30_000L))
                }
            }
            val remaining = queue.getPendingCount()
            if (remaining > 0) {
                syncStatus.update(SyncState.SYNCING, remaining)
            }
        }

        val finalPending = queue.getPendingCount()
        syncStatus.update(
            if (finalPending > 0) SyncState.ERROR else SyncState.IDLE,
            finalPending
        )
    }

    private suspend fun executeSync(entry: SyncEntry) {
        val token = tokenProvider() ?: throw IllegalStateException("No auth token")
        val storeId = if (entry.entityId.contains(":")) entry.entityId.substringBefore(":") else entry.entityId
        val entityId = if (entry.entityId.contains(":")) entry.entityId.substringAfter(":") else entry.entityId
        val url = "$baseUrl/stores/$storeId"

        when (entry.operation) {
            "CREATE" -> {
                client.post("$url/${entry.entityType}") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(entry.payload)
                }
            }
            "UPDATE" -> {
                client.patch("$url/${entry.entityType}/$entityId") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(entry.payload)
                }
            }
            "DELETE" -> {
                client.delete("$url/${entry.entityType}/$entityId") {
                    header("Authorization", "Bearer $token")
                }
            }
        }
    }

    fun stop() {
        syncJob?.cancel()
    }
}
