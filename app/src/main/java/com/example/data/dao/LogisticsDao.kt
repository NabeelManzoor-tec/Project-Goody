package com.example.data.dao

import androidx.room.*
import com.example.data.model.BidEntity
import com.example.data.model.ShipmentEntity
import com.example.data.model.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogisticsDao {

    // --- Shipments ---
    @Query("SELECT * FROM shipments ORDER BY timestamp DESC")
    fun getAllShipments(): Flow<List<ShipmentEntity>>

    @Query("SELECT * FROM shipments WHERE id = :id")
    fun getShipmentById(id: Int): Flow<ShipmentEntity?>

    @Query("SELECT * FROM shipments WHERE id = :id")
    suspend fun getShipmentByIdSync(id: Int): ShipmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipment(shipment: ShipmentEntity): Long

    @Update
    suspend fun updateShipment(shipment: ShipmentEntity)

    @Delete
    suspend fun deleteShipment(shipment: ShipmentEntity)

    // --- Bids ---
    @Query("SELECT * FROM bids ORDER BY timestamp DESC")
    fun getAllBids(): Flow<List<BidEntity>>

    @Query("SELECT * FROM bids WHERE shipmentId = :shipmentId ORDER BY bidAmount ASC")
    fun getBidsForShipment(shipmentId: Int): Flow<List<BidEntity>>

    @Query("SELECT * FROM bids WHERE shipmentId = :shipmentId ORDER BY bidAmount ASC")
    suspend fun getBidsForShipmentSync(shipmentId: Int): List<BidEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBid(bid: BidEntity): Long

    @Update
    suspend fun updateBid(bid: BidEntity)

    @Query("UPDATE bids SET status = :status WHERE id = :bidId")
    suspend fun updateBidStatus(bidId: Int, status: String)

    @Query("UPDATE bids SET status = 'REJECTED' WHERE shipmentId = :shipmentId AND id != :acceptedBidId")
    suspend fun rejectOtherBidsForShipment(shipmentId: Int, acceptedBidId: Int)

    // --- Vehicles ---
    @Query("SELECT * FROM vehicles")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Int): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)
}
