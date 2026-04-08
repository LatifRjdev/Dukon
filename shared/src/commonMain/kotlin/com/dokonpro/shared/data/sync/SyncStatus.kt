package com.dokonpro.shared.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncState {
    IDLE, SYNCING, ERROR
}

data class SyncStatusData(
    val state: SyncState = SyncState.IDLE,
    val pendingCount: Long = 0
)

class SyncStatus {
    private val _status = MutableStateFlow(SyncStatusData())
    val status: StateFlow<SyncStatusData> = _status.asStateFlow()

    fun update(state: SyncState, pendingCount: Long) {
        _status.value = SyncStatusData(state, pendingCount)
    }
}
