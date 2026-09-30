package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Delivery

@Database(
    entities = [Delivery::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FieldCaptureDatabase : RoomDatabase() {

    abstract fun deliveryDao(): DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: FieldCaptureDatabase? = null

        fun getInstance(context: Context): FieldCaptureDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FieldCaptureDatabase::class.java,
                    "fieldcapture_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
