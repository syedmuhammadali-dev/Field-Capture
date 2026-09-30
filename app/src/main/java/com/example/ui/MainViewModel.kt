package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.FieldCaptureDatabase
import com.example.data.model.Delivery
import com.example.data.model.DeliveryCounts
import com.example.data.model.DeliveryStatus
import com.example.data.repository.DeliveryRepository
import com.example.data.storage.PhotoStorageService
import com.example.sync.ConnectionStatus
import com.example.sync.NetworkMonitor
import com.example.sync.NetworkOverrideMode
import com.example.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FieldCaptureDatabase.getInstance(application)
    val repository = DeliveryRepository(database.deliveryDao())
    val photoStorage = PhotoStorageService(application)
    val networkMonitor = NetworkMonitor(application)
    val syncManager = SyncManager(repository, networkMonitor)

    val allDeliveries: StateFlow<List<Delivery>> = repository.allDeliveries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveryCounts: StateFlow<DeliveryCounts> = repository.deliveryCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeliveryCounts())

    val networkStatus: StateFlow<ConnectionStatus> = networkMonitor.effectiveStatus
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
    val statusMessage: StateFlow<String> = syncManager.statusBannerMessage
    val overrideMode: StateFlow<NetworkOverrideMode> = networkMonitor.overrideMode

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _selectedDelivery = MutableStateFlow<Delivery?>(null)
    val selectedDelivery: StateFlow<Delivery?> = _selectedDelivery.asStateFlow()

    fun selectDelivery(delivery: Delivery?) {
        _selectedDelivery.value = delivery
    }

    fun selectDeliveryById(id: String) {
        viewModelScope.launch {
            _selectedDelivery.value = repository.getDeliveryByIdSync(id)
        }
    }

    /**
     * Saves delivery locally into SQLite database.
     * Enforces double-tap protection and offline-first availability.
     */
    fun saveDelivery(
        supplierName: String,
        poNumber: String,
        note: String,
        photoPathOrUri: String,
        onSuccess: (Delivery) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isSaving.value) return // Prevent multiple rapid submissions

        if (supplierName.isBlank()) {
            onError("Supplier name is required")
            return
        }
        if (poNumber.isBlank()) {
            onError("PO number is required")
            return
        }
        if (photoPathOrUri.isBlank()) {
            onError("Delivery ticket photo is required")
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Ensure photo is stored in app internal storage
                val persistentPath = if (photoPathOrUri.startsWith("content://")) {
                    photoStorage.saveImageFromUri(Uri.parse(photoPathOrUri))
                } else {
                    photoPathOrUri
                }

                // 1. Save to SQLite database FIRST (Offline-First)
                val delivery = repository.createDelivery(
                    supplierName = supplierName,
                    poNumber = poNumber,
                    note = note,
                    localPhotoUri = persistentPath
                )

                // 2. If currently online, trigger sync
                if (networkMonitor.isOnline) {
                    syncManager.syncPendingDeliveries()
                }

                onSuccess(delivery)
            } catch (e: Exception) {
                onError("Failed to save delivery locally: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun retryDelivery(deliveryId: String) {
        syncManager.syncSingleDelivery(deliveryId)
    }

    fun retryAll() {
        syncManager.syncPendingDeliveries()
    }

    fun setNetworkOverrideMode(mode: NetworkOverrideMode) {
        networkMonitor.setOverrideMode(mode)
    }

    fun simulateAppKillRecovery(onComplete: (Int) -> Unit) {
        syncManager.testAppKillRecovery(onComplete)
    }

    fun generateSampleTicket(supplier: String, po: String, notes: String): String {
        return photoStorage.generateDeliveryTicket(supplier, po, notes)
    }
}
