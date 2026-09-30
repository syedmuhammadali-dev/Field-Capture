package com.example.data.repository

import com.example.data.database.DeliveryDao
import com.example.data.model.Delivery
import com.example.data.model.DeliveryCounts
import com.example.data.model.DeliveryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class DeliveryRepository(
    private val deliveryDao: DeliveryDao
) {
    val allDeliveries: Flow<List<Delivery>> = deliveryDao.getAllDeliveries()

    fun getDeliveriesByStatus(status: DeliveryStatus): Flow<List<Delivery>> =
        deliveryDao.getDeliveriesByStatus(status)

    fun getDeliveryById(id: String): Flow<Delivery?> =
        deliveryDao.getDeliveryById(id)

    suspend fun getDeliveryByIdSync(id: String): Delivery? =
        deliveryDao.getDeliveryByIdSync(id)

    val deliveryCounts: Flow<DeliveryCounts> = combine(
        deliveryDao.getQueuedCount(),
        deliveryDao.getUploadingCount(),
        deliveryDao.getSyncedCount(),
        deliveryDao.getFailedCount()
    ) { queued, uploading, synced, failed ->
        DeliveryCounts(
            queuedCount = queued,
            uploadingCount = uploading,
            syncedCount = synced,
            failedCount = failed
        )
    }

    suspend fun createDelivery(
        supplierName: String,
        poNumber: String,
        note: String,
        localPhotoUri: String
    ): Delivery {
        val delivery = Delivery(
            id = UUID.randomUUID().toString(),
            supplierName = supplierName.trim(),
            poNumber = poNumber.trim(),
            note = note.trim(),
            localPhotoUri = localPhotoUri,
            status = DeliveryStatus.QUEUED,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncAttempts = 0,
            idempotencyKey = UUID.randomUUID().toString(),
            lastError = null
        )
        deliveryDao.insert(delivery)
        return delivery
    }

    suspend fun updateDelivery(delivery: Delivery) {
        deliveryDao.update(delivery)
    }

    suspend fun getPendingDeliveries(): List<Delivery> =
        deliveryDao.getPendingDeliveries()

    suspend fun recoverInterruptedUploads(): Int =
        deliveryDao.recoverInterruptedUploads()

    suspend fun markUploading(id: String) {
        deliveryDao.updateStatus(id, DeliveryStatus.UPLOADING)
    }

    suspend fun markSynced(id: String, serverId: String) {
        deliveryDao.markSynced(id, serverId)
    }

    suspend fun markFailed(id: String, errorMessage: String) {
        deliveryDao.markFailed(id, errorMessage)
    }

    suspend fun deleteDelivery(id: String) {
        deliveryDao.deleteById(id)
    }
}
