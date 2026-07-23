package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.LogisticsDao
import com.example.data.model.BidEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ShipmentEntity
import com.example.data.model.SupportTicketEntity
import com.example.data.model.VehicleEntity

@Database(
    entities = [ShipmentEntity::class, BidEntity::class, VehicleEntity::class, SupportTicketEntity::class, ChatMessageEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun logisticsDao(): LogisticsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "logistics_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
