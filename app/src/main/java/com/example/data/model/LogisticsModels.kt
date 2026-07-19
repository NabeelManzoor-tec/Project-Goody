package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shipments")
data class ShipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderName: String,
    val receiverName: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val cargoType: String,
    val weightKg: Double,
    val zone: String, // "City-wide", "Country-wide", "Special Economy Zone"
    val basePrice: Double,
    val acceptedBidId: Int? = null,
    val acceptedBidPrice: Double? = null,
    val status: String = "PENDING_BIDS", // "PENDING_BIDS", "MATCHED", "IN_TRANSIT", "DELIVERED"
    val currentProgress: Float = 0.0f, // 0.0f to 1.0f
    val currentLat: Float = 0.0f,
    val currentLng: Float = 0.0f,
    val pickupLat: Float = 0.0f,
    val pickupLng: Float = 0.0f,
    val dropoffLat: Float = 0.0f,
    val dropoffLng: Float = 0.0f,
    val vehicleId: Int? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bids")
data class BidEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shipmentId: Int,
    val vehicleId: Int,
    val driverName: String,
    val plateNumber: String,
    val vehicleType: String,
    val bidAmount: Double,
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "REJECTED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val driverName: String,
    val plateNumber: String,
    val vehicleType: String, // "Box Truck", "Semi Truck", "Refrigerated Van", "Flatbed Trailer"
    val capacityKg: Double,
    val operatingZone: String, // "City-wide", "Country-wide", "Special Economy Zone"
    val isAvailable: Boolean = true,
    val phone: String = "+1 (555) 019-2834"
)
