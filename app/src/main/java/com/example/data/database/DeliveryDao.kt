package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Delivery
import com.example.data.model.DeliveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {

    @Query("SELECT * FROM deliveries ORDER BY createdAt DESC")
    fun getAllDeliveries(): Flow<List<Delivery>>

    @Query("SELECT * FROM deliveries WHERE status = :status ORDER BY createdAt DESC")
    fun getDeliveriesByStatus(status: DeliveryStatus): Flow<List<Delivery>>

    @Query("SELECT * FROM deliveries WHERE id = :id LIMIT 1")
    fun getDeliveryById(id: String): Flow<Delivery?>

    @Query("SELECT * FROM deliveries WHERE id = :id LIMIT 1")
    suspend fun getDeliveryByIdSync(id: String): Delivery?

    @Query("SELECT * FROM deliveries WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getDeliveryByIdempotencyKey(key: String): Delivery?

    @Query("SELECT * FROM deliveries WHERE status IN ('QUEUED', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingDeliveries(): List<Delivery>

    @Query("SELECT * FROM deliveries WHERE status = 'UPLOADING'")
    suspend fun getInterruptedDeliveries(): List<Delivery>

    @Query("""
        UPDATE deliveries 
        SET status = 'QUEUED', 
            lastError = :interruptedError, 
            updatedAt = :now 
        WHERE status = 'UPLOADING'
    """)
    suspend fun recoverInterruptedUploads(
        interruptedError: String = "Upload interrupted by app restart — queued for retry",
        now: Long = System.currentTimeMillis()
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(delivery: Delivery): Long

    @Update
    suspend fun update(delivery: Delivery): Int

    @Query("UPDATE deliveries SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: DeliveryStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE deliveries 
        SET status = 'SYNCED', 
            serverId = :serverId, 
            lastSyncAttempt = :timestamp, 
            updatedAt = :timestamp,
            lastError = NULL 
        WHERE id = :id
    """)
    suspend fun markSynced(id: String, serverId: String, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE deliveries 
        SET status = 'FAILED', 
            syncAttempts = syncAttempts + 1, 
            lastSyncAttempt = :timestamp, 
            lastError = :error, 
            updatedAt = :timestamp 
        WHERE id = :id
    """)
    suspend fun markFailed(id: String, error: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM deliveries WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT COUNT(*) FROM deliveries WHERE status = 'QUEUED'")
    fun getQueuedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM deliveries WHERE status = 'SYNCED'")
    fun getSyncedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM deliveries WHERE status = 'FAILED'")
    fun getFailedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM deliveries WHERE status = 'UPLOADING'")
    fun getUploadingCount(): Flow<Int>
}
