package com.example.sync

import com.example.data.model.Delivery
import com.example.data.model.DeliveryStatus
import com.example.data.repository.DeliveryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class SyncEngineState {
    object Idle : SyncEngineState()
    data class Syncing(val currentItemIndex: Int, val totalItems: Int, val supplierName: String) : SyncEngineState()
    data class Completed(val syncedCount: Int, val failedCount: Int, val timestamp: Long) : SyncEngineState()
    data class Error(val message: String) : SyncEngineState()
}

class SyncManager(
    private val repository: DeliveryRepository,
    private val networkMonitor: NetworkMonitor,
    private val apiClient: ApiClient = ApiClient()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _engineState = MutableStateFlow<SyncEngineState>(SyncEngineState.Idle)
    val engineState: StateFlow<SyncEngineState> = _engineState.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow("Initializing...")
    val statusBannerMessage: StateFlow<String> = _statusBannerMessage.asStateFlow()

    private val _interruptedCountRecovered = MutableStateFlow(0)
    val interruptedCountRecovered: StateFlow<Int> = _interruptedCountRecovered.asStateFlow()

    private var previousConnectionStatus: ConnectionStatus? = null
    private var syncJob: Job? = null

    init {
        // Edge Case: App killed during upload recovery (Section 12)
        // Check for any deliveries stuck in UPLOADING state from a previous crash/kill
        scope.launch {
            val recovered = repository.recoverInterruptedUploads()
            _interruptedCountRecovered.value = recovered
            if (recovered > 0) {
                _statusBannerMessage.value = "Recovered $recovered interrupted uploads — queued for retry"
            }
        }

        // Monitor network state for automatic retry on reconnect (Section 11)
        scope.launch {
            networkMonitor.effectiveStatus.collect { status ->
                val prev = previousConnectionStatus
                previousConnectionStatus = status

                when (status) {
                    ConnectionStatus.ONLINE -> {
                        if (prev == ConnectionStatus.OFFLINE) {
                            _statusBannerMessage.value = "Connection restored — Auto-syncing pending deliveries..."
                            syncPendingDeliveries()
                        } else if (!_isSyncing.value) {
                            _statusBannerMessage.value = "Online — Ready to sync"
                        }
                    }
                    ConnectionStatus.OFFLINE -> {
                        _statusBannerMessage.value = "You're offline — Deliveries will be saved locally"
                    }
                }
            }
        }
    }

    /**
     * Triggers sync for all pending deliveries (QUEUED and FAILED).
     */
    fun syncPendingDeliveries() {
        if (!networkMonitor.isOnline) {
            _statusBannerMessage.value = "Offline — Cannot sync right now"
            return
        }

        syncJob?.cancel()
        syncJob = scope.launch {
            syncMutex.withLock {
                _isSyncing.value = true
                try {
                    val pending = repository.getPendingDeliveries()
                    if (pending.isEmpty()) {
                        _engineState.value = SyncEngineState.Completed(0, 0, System.currentTimeMillis())
                        _statusBannerMessage.value = "All deliveries synced"
                        return@launch
                    }

                    _statusBannerMessage.value = "Syncing ${pending.size} deliveries..."
                    var syncedSuccess = 0
                    var failedCount = 0

                    for ((index, delivery) in pending.withIndex()) {
                        if (!networkMonitor.isOnline) {
                            _statusBannerMessage.value = "Sync paused — connection lost"
                            break
                        }

                        _engineState.value = SyncEngineState.Syncing(
                            currentItemIndex = index + 1,
                            totalItems = pending.size,
                            supplierName = delivery.supplierName
                        )

                        // 1. Mark UPLOADING in local SQLite before network call
                        repository.markUploading(delivery.id)

                        // 2. Perform API upload
                        val result = apiClient.uploadDelivery(delivery, networkMonitor.overrideMode.value)

                        // 3. Update status in local SQLite
                        when (result) {
                            is ApiResult.Success -> {
                                repository.markSynced(delivery.id, result.serverId)
                                syncedSuccess++
                            }
                            is ApiResult.Error -> {
                                repository.markFailed(delivery.id, result.message)
                                failedCount++
                            }
                        }
                    }

                    _engineState.value = SyncEngineState.Completed(syncedSuccess, failedCount, System.currentTimeMillis())
                    _statusBannerMessage.value = when {
                        failedCount > 0 -> "$syncedSuccess synced, $failedCount failed. Tap retry."
                        syncedSuccess > 0 -> "$syncedSuccess deliveries synced successfully"
                        else -> "All deliveries synced"
                    }
                } catch (e: Exception) {
                    _statusBannerMessage.value = "Sync error: ${e.localizedMessage ?: "Unknown error"}"
                    _engineState.value = SyncEngineState.Error(e.localizedMessage ?: "Sync error")
                } finally {
                    _isSyncing.value = false
                }
            }
        }
    }

    /**
     * Retries or syncs a single specific delivery by ID.
     */
    fun syncSingleDelivery(deliveryId: String) {
        if (!networkMonitor.isOnline) {
            _statusBannerMessage.value = "Offline — Connect to retry upload"
            return
        }

        scope.launch {
            syncMutex.withLock {
                val delivery = repository.getDeliveryByIdSync(deliveryId) ?: return@launch
                _isSyncing.value = true
                _statusBannerMessage.value = "Uploading ${delivery.supplierName}..."

                try {
                    repository.markUploading(delivery.id)
                    val result = apiClient.uploadDelivery(delivery, networkMonitor.overrideMode.value)

                    when (result) {
                        is ApiResult.Success -> {
                            repository.markSynced(delivery.id, result.serverId)
                            _statusBannerMessage.value = "Delivery ${delivery.poNumber} synced successfully"
                        }
                        is ApiResult.Error -> {
                            repository.markFailed(delivery.id, result.message)
                            _statusBannerMessage.value = "Upload failed for ${delivery.poNumber}: ${result.message}"
                        }
                    }
                } finally {
                    _isSyncing.value = false
                }
            }
        }
    }

    /**
     * Simulates the App Killed During Upload edge case for testing (Section 12 & 22).
     * Marks the first pending delivery as UPLOADING, then triggers recovery logic.
     */
    fun testAppKillRecovery(onComplete: (recoveredCount: Int) -> Unit = {}) {
        scope.launch {
            val pending = repository.getPendingDeliveries()
            if (pending.isNotEmpty()) {
                val target = pending.first()
                repository.markUploading(target.id)
                delay(300)
            }
            val recovered = repository.recoverInterruptedUploads()
            _interruptedCountRecovered.value = recovered
            _statusBannerMessage.value = "Simulated crash recovery: $recovered interrupted uploads recovered to QUEUED"
            onComplete(recovered)
        }
    }

    fun getApiClient(): ApiClient = apiClient
}
