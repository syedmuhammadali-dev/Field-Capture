package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.DeliveryStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: DeliveryStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): DeliveryStatus {
        return try {
            DeliveryStatus.valueOf(value)
        } catch (e: Exception) {
            DeliveryStatus.QUEUED
        }
    }
}
