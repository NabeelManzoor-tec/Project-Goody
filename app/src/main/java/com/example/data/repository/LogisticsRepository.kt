package com.example.data.repository

import com.example.data.dao.LogisticsDao
import com.example.data.model.BidEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ShipmentEntity
import com.example.data.model.SupportTicketEntity
import com.example.data.model.VehicleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LogisticsRepository(private val logisticsDao: LogisticsDao) {

    val allShipments: Flow<List<ShipmentEntity>> = logisticsDao.getAllShipments()
    val allBids: Flow<List<BidEntity>> = logisticsDao.getAllBids()
    val allVehicles: Flow<List<VehicleEntity>> = logisticsDao.getAllVehicles()
    val allSupportTickets: Flow<List<SupportTicketEntity>> = logisticsDao.getAllSupportTickets()
    val allChatMessages: Flow<List<ChatMessageEntity>> = logisticsDao.getAllChatMessages()

    fun getMessagesForShipment(shipmentId: Int): Flow<List<ChatMessageEntity>> {
        return logisticsDao.getMessagesForShipment(shipmentId)
    }

    suspend fun insertChatMessage(message: ChatMessageEntity): Long {
        return logisticsDao.insertChatMessage(message)
    }

    suspend fun markChatMessagesRead(shipmentId: Int, currentUserName: String) {
        logisticsDao.markChatMessagesRead(shipmentId, currentUserName)
    }

    fun getSupportTicketsByUserEmail(email: String): Flow<List<SupportTicketEntity>> {
        return logisticsDao.getSupportTicketsByUserEmail(email)
    }

    suspend fun insertSupportTicket(ticket: SupportTicketEntity): Long {
        return logisticsDao.insertSupportTicket(ticket)
    }

    suspend fun respondToSupportTicket(ticketId: Int, status: String, response: String) {
        logisticsDao.respondToSupportTicket(ticketId, status, response)
    }

    fun getShipmentById(id: Int): Flow<ShipmentEntity?> {
        return logisticsDao.getShipmentById(id)
    }

    fun getBidsForShipment(shipmentId: Int): Flow<List<BidEntity>> {
        return logisticsDao.getBidsForShipment(shipmentId)
    }

    suspend fun insertShipment(shipment: ShipmentEntity): Long {
        return logisticsDao.insertShipment(shipment)
    }

    suspend fun updateShipment(shipment: ShipmentEntity) {
        logisticsDao.updateShipment(shipment)
    }

    suspend fun deleteShipment(shipment: ShipmentEntity) {
        logisticsDao.deleteShipment(shipment)
    }

    suspend fun insertBid(bid: BidEntity): Long {
        return logisticsDao.insertBid(bid)
    }

    suspend fun updateBid(bid: BidEntity) {
        logisticsDao.updateBid(bid)
    }

    suspend fun acceptBid(shipmentId: Int, bidId: Int) {
        // Find the bid
        val bids = logisticsDao.getBidsForShipmentSync(shipmentId)
        val acceptedBid = bids.find { it.id == bidId } ?: return

        // Update other bids to REJECTED
        logisticsDao.rejectOtherBidsForShipment(shipmentId, bidId)

        // Update accepted bid status to ACCEPTED
        logisticsDao.updateBidStatus(bidId, "ACCEPTED")

        // Retrieve vehicle details and mark as unavailable
        val vehicle = logisticsDao.getVehicleById(acceptedBid.vehicleId)
        if (vehicle != null) {
            logisticsDao.updateVehicle(vehicle.copy(isAvailable = false))
        }

        // Find the shipment and update its status
        val shipment = logisticsDao.getShipmentByIdSync(shipmentId)
        if (shipment != null) {
            val updatedShipment = shipment.copy(
                acceptedBidId = bidId,
                acceptedBidPrice = acceptedBid.bidAmount,
                status = "MATCHED",
                vehicleId = acceptedBid.vehicleId,
                driverName = acceptedBid.driverName,
                driverPhone = vehicle?.phone ?: "+1 (555) 019-2834",
                currentLat = shipment.pickupLat,
                currentLng = shipment.pickupLng,
                currentProgress = 0.0f
            )
            logisticsDao.updateShipment(updatedShipment)
        }
    }

    suspend fun insertVehicle(vehicle: VehicleEntity): Long {
        return logisticsDao.insertVehicle(vehicle)
    }

    suspend fun updateVehicle(vehicle: VehicleEntity) {
        logisticsDao.updateVehicle(vehicle)
    }

    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        logisticsDao.deleteVehicle(vehicle)
    }

    suspend fun prepopulateIfEmpty() {
        val vehiclesList = allVehicles.first()
        if (vehiclesList.isEmpty()) {
            // 1. Insert Sample Vehicles
            val v1Id = logisticsDao.insertVehicle(
                VehicleEntity(
                    driverName = "Dave Miller",
                    plateNumber = "TX-482-9B",
                    vehicleType = "Semi Truck",
                    capacityKg = 18000.0,
                    operatingZone = "Country-wide",
                    isAvailable = true,
                    phone = "+1 (555) 762-1082"
                )
            ).toInt()

            val v2Id = logisticsDao.insertVehicle(
                VehicleEntity(
                    driverName = "Sarah Connor",
                    plateNumber = "CA-901-7F",
                    vehicleType = "Refrigerated Van",
                    capacityKg = 4500.0,
                    operatingZone = "Special Economy Zone",
                    isAvailable = true,
                    phone = "+1 (555) 438-9017"
                )
            ).toInt()

            val v3Id = logisticsDao.insertVehicle(
                VehicleEntity(
                    driverName = "John Doe",
                    plateNumber = "NY-552-3A",
                    vehicleType = "Box Truck",
                    capacityKg = 3000.0,
                    operatingZone = "City-wide",
                    isAvailable = true,
                    phone = "+1 (555) 231-5523"
                )
            ).toInt()

            val v4Id = logisticsDao.insertVehicle(
                VehicleEntity(
                    driverName = "Elena Rostova",
                    plateNumber = "IL-731-8D",
                    vehicleType = "Flatbed Trailer",
                    capacityKg = 12000.0,
                    operatingZone = "Country-wide",
                    isAvailable = true,
                    phone = "+1 (555) 662-7318"
                )
            ).toInt()

            // 2. Insert Sample Shipments
            // Shipment 1: Active In-Transit shipment (matched to Sarah Connor)
            val s1Id = logisticsDao.insertShipment(
                ShipmentEntity(
                    senderName = "EcoTech Industries",
                    receiverName = "Alpha Distribs",
                    pickupLocation = "SEZ Port Terminal A",
                    dropoffLocation = "Industrial Park Gate 4",
                    cargoType = "Industrial Turbines",
                    weightKg = 4000.0,
                    zone = "Special Economy Zone",
                    basePrice = 1200.0,
                    status = "IN_TRANSIT",
                    currentProgress = 0.45f,
                    pickupLat = 1.35f,
                    pickupLng = 103.80f,
                    currentLat = 1.3185f,
                    currentLng = 103.8225f,
                    dropoffLat = 1.28f,
                    dropoffLng = 103.85f,
                    vehicleId = v2Id,
                    driverName = "Sarah Connor",
                    driverPhone = "+1 (555) 438-9017"
                )
            ).toInt()

            // Shipment 2: Shipment under bidding (No accepted bid yet)
            val s2Id = logisticsDao.insertShipment(
                ShipmentEntity(
                    senderName = "Apex Hardware Corp",
                    receiverName = "Metro Build Site",
                    pickupLocation = "Steel Works East",
                    dropoffLocation = "Grand Plaza Site",
                    cargoType = "Steel Beams",
                    weightKg = 15000.0,
                    zone = "Country-wide",
                    basePrice = 2400.0,
                    status = "PENDING_BIDS",
                    currentProgress = 0.0f,
                    pickupLat = 40.71f,
                    pickupLng = -74.00f,
                    currentLat = 40.71f,
                    currentLng = -74.00f,
                    dropoffLat = 34.05f,
                    dropoffLng = -118.24f
                )
            ).toInt()

            // Shipment 3: Local City-wide shipment that just got booked
            val s3Id = logisticsDao.insertShipment(
                ShipmentEntity(
                    senderName = "Fresh Farms Organic",
                    receiverName = "Central Supermarket",
                    pickupLocation = "Green Valley Farm",
                    dropoffLocation = "Downtown Grocery Hub",
                    cargoType = "Fresh Berries",
                    weightKg = 1200.0,
                    zone = "City-wide",
                    basePrice = 350.0,
                    status = "PENDING_BIDS",
                    currentProgress = 0.0f,
                    pickupLat = 34.05f,
                    pickupLng = -118.24f,
                    currentLat = 34.05f,
                    currentLng = -118.24f,
                    dropoffLat = 34.12f,
                    dropoffLng = -118.30f
                )
            ).toInt()

            // Shipment 4: Completed shipment
            val s4Id = logisticsDao.insertShipment(
                ShipmentEntity(
                    senderName = "Tech Assembly Ltd",
                    receiverName = "Global Logistics HQ",
                    pickupLocation = "Silicon Valley Block C",
                    dropoffLocation = "San Jose Airport Cargo",
                    cargoType = "Semiconductors",
                    weightKg = 800.0,
                    zone = "City-wide",
                    basePrice = 450.0,
                    status = "DELIVERED",
                    currentProgress = 1.0f,
                    pickupLat = 37.33f,
                    pickupLng = -121.88f,
                    currentLat = 37.37f,
                    currentLng = -121.92f,
                    dropoffLat = 37.37f,
                    dropoffLng = -121.92f,
                    vehicleId = v3Id,
                    driverName = "John Doe",
                    driverPhone = "+1 (555) 231-5523",
                    acceptedBidPrice = 420.0
                )
            ).toInt()

            // 3. Insert Sample Bids
            // Bids for Shipment 2 (Steel Beams, base price 2400)
            logisticsDao.insertBid(
                BidEntity(
                    shipmentId = s2Id,
                    vehicleId = v1Id,
                    driverName = "Dave Miller",
                    plateNumber = "TX-482-9B",
                    vehicleType = "Semi Truck",
                    bidAmount = 2300.0,
                    status = "PENDING"
                )
            )
            logisticsDao.insertBid(
                BidEntity(
                    shipmentId = s2Id,
                    vehicleId = v4Id,
                    driverName = "Elena Rostova",
                    plateNumber = "IL-731-8D",
                    vehicleType = "Flatbed Trailer",
                    bidAmount = 2150.0,
                    status = "PENDING"
                )
            )

            // Bids for Shipment 3 (Fresh Berries, base price 350)
            logisticsDao.insertBid(
                BidEntity(
                    shipmentId = s3Id,
                    vehicleId = v3Id,
                    driverName = "John Doe",
                    plateNumber = "NY-552-3A",
                    vehicleType = "Box Truck",
                    bidAmount = 330.0,
                    status = "PENDING"
                )
            )

            // Bids for Shipment 4 (Completed Semiconductor shipment)
            logisticsDao.insertBid(
                BidEntity(
                    shipmentId = s4Id,
                    vehicleId = v3Id,
                    driverName = "John Doe",
                    plateNumber = "NY-552-3A",
                    vehicleType = "Box Truck",
                    bidAmount = 420.0,
                    status = "ACCEPTED"
                )
            )
            // 4. Insert Seed Support Tickets
            logisticsDao.insertSupportTicket(
                com.example.data.model.SupportTicketEntity(
                    userRole = "VEHICLE_OWNER",
                    userName = "Dave Miller (Fleet Driver)",
                    userEmail = "driver@logistics.com",
                    userPhone = "+1 (555) 762-1082",
                    shipmentId = s1Id,
                    issueCategory = "Route & Address Help",
                    priority = "HIGH",
                    description = "Heavy construction on Interstate 80 causing 45 min delay. Requesting updated dock clearance window at SEZ Gate 4.",
                    status = "IN_PROGRESS",
                    supportResponse = "Dispatch Ops notified SEZ Gate 4 manager. Your arrival window has been extended to 16:30 hrs."
                )
            )

            logisticsDao.insertSupportTicket(
                com.example.data.model.SupportTicketEntity(
                    userRole = "CONSIGNEE",
                    userName = "EcoTech Shipper Corp",
                    userEmail = "shipper@logistics.com",
                    userPhone = "+1 (555) 892-1001",
                    shipmentId = s2Id,
                    issueCategory = "Cargo Damage",
                    priority = "CRITICAL",
                    description = "Requesting temperature verification report for refrigerated cargo transit before offloading at Central Warehouse.",
                    status = "OPEN",
                    supportResponse = null
                )
            )

            // 5. Insert Seed Chat Messages between Drivers and Consignees
            logisticsDao.insertChatMessage(
                ChatMessageEntity(
                    shipmentId = s1Id,
                    senderRole = "SYSTEM",
                    senderName = "System Logistics Core",
                    receiverName = "EcoTech Shipper Corp",
                    message = "💬 Channel Created: Driver Sarah Connor accepted cargo for Industrial Turbines.",
                    messageType = "SYSTEM"
                )
            )

            logisticsDao.insertChatMessage(
                ChatMessageEntity(
                    shipmentId = s1Id,
                    senderRole = "DRIVER",
                    senderName = "Sarah Connor",
                    receiverName = "EcoTech Shipper Corp",
                    message = "Hello! I am loading the Industrial Turbines in SEZ Port Terminal A now. Temperature controls are active at 4°C.",
                    messageType = "TEXT"
                )
            )

            logisticsDao.insertChatMessage(
                ChatMessageEntity(
                    shipmentId = s1Id,
                    senderRole = "CONSIGNEE",
                    senderName = "EcoTech Shipper Corp",
                    receiverName = "Sarah Connor",
                    message = "Great! Please ask for Supervisor Mark at Industrial Park Gate 4 upon arrival.",
                    messageType = "TEXT"
                )
            )

            logisticsDao.insertChatMessage(
                ChatMessageEntity(
                    shipmentId = s2Id,
                    senderRole = "SYSTEM",
                    senderName = "System Logistics Core",
                    receiverName = "Apex Hardware Corp",
                    message = "💬 Bid Discussion Channel Opened: Driver Dave Miller placed a bid of $2,250.00.",
                    messageType = "SYSTEM"
                )
            )

            logisticsDao.insertChatMessage(
                ChatMessageEntity(
                    shipmentId = s2Id,
                    senderRole = "DRIVER",
                    senderName = "Dave Miller",
                    receiverName = "Apex Hardware Corp",
                    message = "Hi! I have a Semi Truck with 18T capacity available in Steel Works East. Can pick up within 30 minutes of bid acceptance.",
                    messageType = "TEXT"
                )
            )
        }
    }
}
