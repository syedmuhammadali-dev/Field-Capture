package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class DeliveryStatus {
    QUEUED,     // Saved locally, waiting for connection
    UPLOADING,  // Upload in progress
    SYNCED,     // Successfully synced to backend
    FAILED      // Upload failed, pending retry
}

@Entity(
    tableName = "deliveries",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["status"])
    ]
)
data class Delivery(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val supplierName: String,
    val poNumber: String,
    val note: String = "",
    val localPhotoUri: String,
    val status: DeliveryStatus = DeliveryStatus.QUEUED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null,
    val serverId: String? = null,
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val lastError: String? = null
)

data class DeliveryCounts(
    val queuedCount: Int = 0,
    val uploadingCount: Int = 0,
    val syncedCount: Int = 0,
    val failedCount: Int = 0
) {
    val total: Int get() = queuedCount + uploadingCount + syncedCount + failedCount
    val pendingCount: Int get() = queuedCount + failedCount
}
