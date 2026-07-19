package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.BidEntity
import com.example.data.model.ShipmentEntity
import com.example.data.model.VehicleEntity
import com.example.data.repository.LogisticsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class UserRole {
    ADMIN,
    VEHICLE_OWNER,
    CONSIGNEE
}

enum class Screen {
    DASHBOARD,
    ACTIVE_SHIPMENTS,
    BILLING,
    SETTINGS
}

class LogisticsViewModel(
    application: Application,
    private val repository: LogisticsRepository
) : AndroidViewModel(application) {

    // Current State
    private val _currentRole = MutableStateFlow(UserRole.CONSIGNEE)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedShipmentId = MutableStateFlow<Int?>(null)
    val selectedShipmentId: StateFlow<Int?> = _selectedShipmentId.asStateFlow()

    // Database Flows
    val shipments: StateFlow<List<ShipmentEntity>> = repository.allShipments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bids: StateFlow<List<BidEntity>> = repository.allBids
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehicles: StateFlow<List<VehicleEntity>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected vehicle for Vehicle Owner role
    private val _selectedVehicleId = MutableStateFlow<Int?>(null)
    val selectedVehicleId: StateFlow<Int?> = _selectedVehicleId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            // Default to first vehicle for Owner role if available
            repository.allVehicles.first().firstOrNull()?.let {
                _selectedVehicleId.value = it.id
            }
        }
    }

    // Role switcher
    fun setRole(role: UserRole) {
        _currentRole.value = role
        // Auto-select vehicle when switching to Vehicle Owner
        if (role == UserRole.VEHICLE_OWNER && _selectedVehicleId.value == null) {
            viewModelScope.launch {
                vehicles.value.firstOrNull()?.let {
                    _selectedVehicleId.value = it.id
                }
            }
        }
    }

    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectShipment(shipmentId: Int?) {
        _selectedShipmentId.value = shipmentId
    }

    fun selectVehicle(vehicleId: Int) {
        _selectedVehicleId.value = vehicleId
    }

    fun getBidsForShipment(shipmentId: Int): Flow<List<BidEntity>> {
        return repository.getBidsForShipment(shipmentId)
    }

    // --- Action Methods ---

    // 1. Create Shipment (Consignee Book Shipment)
    fun createShipment(
        sender: String,
        receiver: String,
        pickup: String,
        dropoff: String,
        cargo: String,
        weight: Double,
        zone: String,
        basePrice: Double,
        autoMatch: Boolean
    ) {
        viewModelScope.launch {
            // Coordinate mock generation based on zone
            val (pLat, pLng, dLat, dLng) = when (zone) {
                "City-wide" -> Quadruplet(34.05f, -118.24f, 34.12f, -118.30f)
                "Special Economy Zone" -> Quadruplet(1.35f, 103.80f, 1.28f, 103.85f)
                else -> Quadruplet(40.71f, -74.00f, 34.05f, -118.24f) // Country-wide
            }

            val newShipment = ShipmentEntity(
                senderName = sender,
                receiverName = receiver,
                pickupLocation = pickup,
                dropoffLocation = dropoff,
                cargoType = cargo,
                weightKg = weight,
                zone = zone,
                basePrice = basePrice,
                status = "PENDING_BIDS",
                pickupLat = pLat,
                pickupLng = pLng,
                currentLat = pLat,
                currentLng = pLng,
                dropoffLat = dLat,
                dropoffLng = dLng
            )

            val shipmentId = repository.insertShipment(newShipment).toInt()

            if (autoMatch) {
                // Trigger automated load matching algorithm
                runAutomatedMatching(shipmentId)
            }
        }
    }

    // 2. Automated Load Matching Algorithm
    fun runAutomatedMatching(shipmentId: Int) {
        viewModelScope.launch {
            val allCurrentShipments = shipments.value
            val shipment = allCurrentShipments.find { it.id == shipmentId } ?: return@launch
            if (shipment.status != "PENDING_BIDS") return@launch

            // Find an available vehicle that:
            // - Operates in the shipment's zone
            // - Has capacity >= weight of shipment
            val allCurrentVehicles = vehicles.value
            val matchedVehicle = allCurrentVehicles.find { vehicle ->
                vehicle.isAvailable &&
                vehicle.operatingZone == shipment.zone &&
                vehicle.capacityKg >= shipment.weightKg
            }

            if (matchedVehicle != null) {
                // Auto-create an accepted bid
                val bidAmt = shipment.basePrice * 0.95 // 5% discount for auto-match
                val autoBidId = repository.insertBid(
                    BidEntity(
                        shipmentId = shipmentId,
                        vehicleId = matchedVehicle.id,
                        driverName = matchedVehicle.driverName,
                        plateNumber = matchedVehicle.plateNumber,
                        vehicleType = matchedVehicle.vehicleType,
                        bidAmount = bidAmt,
                        status = "ACCEPTED"
                    )
                ).toInt()

                // Link them
                val updated = shipment.copy(
                    acceptedBidId = autoBidId,
                    acceptedBidPrice = bidAmt,
                    status = "MATCHED",
                    vehicleId = matchedVehicle.id,
                    driverName = matchedVehicle.driverName,
                    driverPhone = matchedVehicle.phone,
                    currentLat = shipment.pickupLat,
                    currentLng = shipment.pickupLng,
                    currentProgress = 0.0f
                )
                repository.updateShipment(updated)

                // Update vehicle state
                repository.updateVehicle(matchedVehicle.copy(isAvailable = false))
            }
        }
    }

    // 3. Bid Placing System (Vehicle Owners bidding)
    fun placeBid(shipmentId: Int, vehicleId: Int, bidAmount: Double) {
        viewModelScope.launch {
            val vehicle = vehicles.value.find { it.id == vehicleId } ?: return@launch
            val bid = BidEntity(
                shipmentId = shipmentId,
                vehicleId = vehicleId,
                driverName = vehicle.driverName,
                plateNumber = vehicle.plateNumber,
                vehicleType = vehicle.vehicleType,
                bidAmount = bidAmount,
                status = "PENDING"
            )
            repository.insertBid(bid)
        }
    }

    // 4. Accept Bid System (Consignee accepts carrier's bid)
    fun acceptBid(shipmentId: Int, bidId: Int) {
        viewModelScope.launch {
            repository.acceptBid(shipmentId, bidId)

            // Mark vehicle as unavailable
            val bid = bids.value.find { it.id == bidId }
            if (bid != null) {
                val vehicle = vehicles.value.find { it.id == bid.vehicleId }
                if (vehicle != null) {
                    repository.updateVehicle(vehicle.copy(isAvailable = false))
                }
            }
        }
    }

    // 5. Driver Simulation: Change delivery status / location
    fun startShipmentTransit(shipmentId: Int) {
        viewModelScope.launch {
            val shipment = shipments.value.find { it.id == shipmentId } ?: return@launch
            if (shipment.status == "MATCHED") {
                val updated = shipment.copy(
                    status = "IN_TRANSIT",
                    currentProgress = 0.05f
                )
                repository.updateShipment(updated)
            }
        }
    }

    fun simulateTransitLocationUpdate(shipmentId: Int) {
        viewModelScope.launch {
            val shipment = shipments.value.find { it.id == shipmentId } ?: return@launch
            if (shipment.status == "IN_TRANSIT") {
                val nextProgress = (shipment.currentProgress + 0.15f).coerceAtMost(1.0f)
                val isDelivered = nextProgress >= 1.0f

                val nextLat = shipment.pickupLat + nextProgress * (shipment.dropoffLat - shipment.pickupLat)
                val nextLng = shipment.pickupLng + nextProgress * (shipment.dropoffLng - shipment.pickupLng)

                val updated = shipment.copy(
                    currentProgress = nextProgress,
                    currentLat = nextLat,
                    currentLng = nextLng,
                    status = if (isDelivered) "DELIVERED" else "IN_TRANSIT"
                )
                repository.updateShipment(updated)

                // If delivered, release the vehicle availability back to true
                if (isDelivered && shipment.vehicleId != null) {
                    val vehicle = vehicles.value.find { it.id == shipment.vehicleId }
                    if (vehicle != null) {
                        repository.updateVehicle(vehicle.copy(isAvailable = true))
                    }
                }
            }
        }
    }

    fun markShipmentDelivered(shipmentId: Int) {
        viewModelScope.launch {
            val shipment = shipments.value.find { it.id == shipmentId } ?: return@launch
            val updated = shipment.copy(
                status = "DELIVERED",
                currentProgress = 1.0f,
                currentLat = shipment.dropoffLat,
                currentLng = shipment.dropoffLng
            )
            repository.updateShipment(updated)

            if (shipment.vehicleId != null) {
                val vehicle = vehicles.value.find { it.id == shipment.vehicleId }
                if (vehicle != null) {
                    repository.updateVehicle(vehicle.copy(isAvailable = true))
                }
            }
        }
    }

    // 6. Register a new vehicle
    fun registerVehicle(driver: String, plate: String, type: String, capacity: Double, zone: String) {
        viewModelScope.launch {
            val vehicle = VehicleEntity(
                driverName = driver,
                plateNumber = plate,
                vehicleType = type,
                capacityKg = capacity,
                operatingZone = zone,
                isAvailable = true
            )
            repository.insertVehicle(vehicle)
        }
    }

    // 7. Delete Shipment (Admin control)
    fun deleteShipment(shipment: ShipmentEntity) {
        viewModelScope.launch {
            repository.deleteShipment(shipment)
        }
    }

    // 8. Reset database
    fun resetDatabase() {
        viewModelScope.launch {
            // Delete all shipments, vehicles, and bids
            shipments.value.forEach { repository.deleteShipment(it) }
            vehicles.value.forEach { repository.deleteVehicle(it) }
            // Re-prepopulate
            repository.prepopulateIfEmpty()
        }
    }

    data class Quadruplet(val first: Float, val second: Float, val third: Float, val fourth: Float)
}

class LogisticsViewModelFactory(
    private val application: Application,
    private val repository: LogisticsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogisticsViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
