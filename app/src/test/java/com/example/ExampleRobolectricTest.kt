package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.FieldCaptureDatabase
import com.example.data.model.Delivery
import com.example.data.model.DeliveryStatus
import com.example.data.repository.DeliveryRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: FieldCaptureDatabase
    private lateinit var repository: DeliveryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FieldCaptureDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DeliveryRepository(database.deliveryDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `read string from context verifies FieldCapture app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FieldCapture", appName)
    }

    @Test
    fun `new delivery is saved locally first as QUEUED with unique idempotencyKey`() = runBlocking {
        val delivery = repository.createDelivery(
            supplierName = "Acme Concrete",
            poNumber = "PO-1029",
            note = "Slump 4.5 inch test passed",
            localPhotoUri = "/data/user/0/ticket_123.jpg"
        )

        assertNotNull(delivery.id)
        assertNotNull(delivery.idempotencyKey)
        assertEquals(DeliveryStatus.QUEUED, delivery.status)
        assertEquals("Acme Concrete", delivery.supplierName)

        val retrieved = repository.getDeliveryByIdSync(delivery.id)
        assertNotNull(retrieved)
        assertEquals(delivery.idempotencyKey, retrieved?.idempotencyKey)
    }

    @Test
    fun `app restart recovers interrupted uploads from UPLOADING back to QUEUED`() = runBlocking {
        // 1. Create a delivery
        val delivery = repository.createDelivery(
            supplierName = "Vulcan Materials",
            poNumber = "PO-8821",
            note = "Delivery in progress when app killed",
            localPhotoUri = "/data/user/0/ticket_456.jpg"
        )

        // 2. Simulate app marking delivery as UPLOADING mid-flight
        repository.markUploading(delivery.id)
        val uploadingRecord = repository.getDeliveryByIdSync(delivery.id)
        assertEquals(DeliveryStatus.UPLOADING, uploadingRecord?.status)

        // 3. Simulate app restart recovery
        val recoveredCount = repository.recoverInterruptedUploads()
        assertEquals(1, recoveredCount)

        // 4. Verify status reverted safely to QUEUED with recovery error note
        val recoveredRecord = repository.getDeliveryByIdSync(delivery.id)
        assertEquals(DeliveryStatus.QUEUED, recoveredRecord?.status)
        assertTrue(recoveredRecord?.lastError?.contains("interrupted") == true)
    }

    @Test
    fun `idempotency keys are unique per created delivery`() = runBlocking {
        val d1 = repository.createDelivery("Supplier A", "PO-1", "Note 1", "/p1.jpg")
        val d2 = repository.createDelivery("Supplier B", "PO-2", "Note 2", "/p2.jpg")

        assertNotEquals(d1.idempotencyKey, d2.idempotencyKey)
    }
}
