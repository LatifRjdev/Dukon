package com.dokonpro.shared.data.sync

import com.dokonpro.shared.db.DokonProDatabase
import kotlinx.datetime.Clock

data class SyncEntry(
    val id: Long,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val retryCount: Long,
    val status: String,
    val createdAt: String
)

class SyncQueue(private val db: DokonProDatabase) {

    fun enqueue(entityType: String, entityId: String, operation: String, payload: String) {
        db.sync_queueQueries.insert(
            entity_type = entityType,
            entity_id = entityId,
            operation = operation,
            payload = payload,
            created_at = Clock.System.now().toString()
        )
    }

    fun getPending(): List<SyncEntry> =
        db.sync_queueQueries.selectPending().executeAsList().map { row ->
            SyncEntry(
                id = row.id,
                entityType = row.entity_type,
                entityId = row.entity_id,
                operation = row.operation,
                payload = row.payload,
                retryCount = row.retry_count,
                status = row.status,
                createdAt = row.created_at
            )
        }

    fun getPendingCount(): Long =
        db.sync_queueQueries.selectPendingCount().executeAsOne()

    fun markCompleted(id: Long) {
        db.sync_queueQueries.deleteById(id)
    }

    fun markFailed(id: Long, retryCount: Long) {
        val newStatus = if (retryCount >= 5) "FAILED" else "PENDING"
        db.sync_queueQueries.updateStatus(status = newStatus, retry_count = retryCount, id = id)
    }

    fun cleanCompleted() {
        db.sync_queueQueries.deleteCompleted()
    }
}
