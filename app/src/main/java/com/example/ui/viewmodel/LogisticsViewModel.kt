package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

data class MilestoneAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val shipmentId: Int,
    val cargoType: String,
    val oldStatus: String,
    val newStatus: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class UserAccount(
    val id: Int = 1,
    val name: String,
    val email: String,
    val role: UserRole,
    val phone: String = "+1 (555) 019-2834",
    val companyOrVehicle: String = "Global Freight Logistics"
)

enum class UserRole {
    ADMIN,
    VEHICLE_OWNER,
    CONSIGNEE
}

enum class Screen {
    DASHBOARD,
    ACTIVE_SHIPMENTS,
    SUPPORT,
    BILLING,
    SETTINGS
}

class LogisticsViewModel(
    application: Application,
    private val repository: LogisticsRepository
) : AndroidViewModel(application) {

    // Pre-configured Demo Accounts
    val demoAccounts = mapOf(
        UserRole.CONSIGNEE to UserAccount(
            id = 1,
            name = "EcoTech Shipper Corp",
            email = "shipper@logistics.com",
            role = UserRole.CONSIGNEE,
            phone = "+1 (555) 892-1001",
            companyOrVehicle = "EcoTech Global Imports"
        ),
        UserRole.VEHICLE_OWNER to UserAccount(
            id = 2,
            name = "Dave Miller (Fleet Driver)",
            email = "driver@logistics.com",
            role = UserRole.VEHICLE_OWNER,
            phone = "+1 (555) 762-1082",
            companyOrVehicle = "Semi Truck (TX-482-9B)"
        ),
        UserRole.ADMIN to UserAccount(
            id = 3,
            name = "System Administrator",
            email = "admin@logistics.com",
            role = UserRole.ADMIN,
            phone = "+1 (555) 000-0000",
            companyOrVehicle = "Zone Command Center HQ"
        )
    )

    // FirebaseAuth Service Integration
    private val firebaseAuthService = com.example.data.auth.FirebaseAuthService()

    // Current State
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.CONSIGNEE)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedShipmentId = MutableStateFlow<Int?>(null)
    val selectedShipmentId: StateFlow<Int?> = _selectedShipmentId.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // Shipper Milestone Notifications Engine
    private val _milestoneAlerts = MutableStateFlow<List<MilestoneAlert>>(emptyList())
    val milestoneAlerts: StateFlow<List<MilestoneAlert>> = _milestoneAlerts.asStateFlow()

    val unreadMilestoneCount: StateFlow<Int> = _milestoneAlerts
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val previousStatusMap = mutableMapOf<Int, String>()

    // Database Flows
    val shipments: StateFlow<List<ShipmentEntity>> = repository.allShipments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bids: StateFlow<List<BidEntity>> = repository.allBids
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehicles: StateFlow<List<VehicleEntity>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supportTickets: StateFlow<List<com.example.data.model.SupportTicketEntity>> = repository.allSupportTickets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChatMessages: StateFlow<List<com.example.data.model.ChatMessageEntity>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadChatCount: StateFlow<Int> = allChatMessages
        .map { list ->
            val curUser = currentUser.value?.name ?: ""
            list.count { !it.isRead && it.senderName != curUser }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Selected vehicle for Vehicle Owner role
    private val _selectedVehicleId = MutableStateFlow<Int?>(null)
    val selectedVehicleId: StateFlow<Int?> = _selectedVehicleId.asStateFlow()

    init {
        createNotificationChannel()
        setupShipmentMilestoneObserver()

        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            // Default to first vehicle for Owner role if available
            repository.allVehicles.first().firstOrNull()?.let {
                _selectedVehicleId.value = it.id
            }

            // Check if user is already signed in via Firebase
            firebaseAuthService.getCurrentFirebaseUser()?.let { existingUser ->
                _currentUser.value = existingUser
                _currentRole.value = existingUser.role
            }
        }
    }

    // --- Authentication & Portal Switching ---

    fun quickDemoLogin(role: UserRole) {
        val account = demoAccounts[role]
        _currentUser.value = account
        _currentRole.value = role
        if (role == UserRole.VEHICLE_OWNER && _selectedVehicleId.value == null) {
            viewModelScope.launch {
                vehicles.value.firstOrNull()?.let {
                    _selectedVehicleId.value = it.id
                }
            }
        }
    }

    fun signIn(email: String, password: String = "Password123!", role: UserRole) {
        viewModelScope.launch {
            _syncMessage.value = "Authenticating with Firebase Auth..."
            val user = firebaseAuthService.signIn(email, password, role)
            _currentUser.value = user
            _currentRole.value = role
            _syncMessage.value = "Successfully signed in to ${when(role) {
                UserRole.CONSIGNEE -> "Shipper Portal"
                UserRole.VEHICLE_OWNER -> "Driver Portal"
                UserRole.ADMIN -> "Admin Control Center"
            }}"
            if (role == UserRole.VEHICLE_OWNER && _selectedVehicleId.value == null) {
                vehicles.value.firstOrNull()?.let {
                    _selectedVehicleId.value = it.id
                }
            }
            kotlinx.coroutines.delay(2000)
            _syncMessage.value = null
        }
    }

    fun signUp(name: String, email: String, password: String = "Password123!", role: UserRole, phone: String, companyOrVehicle: String) {
        viewModelScope.launch {
            _syncMessage.value = "Creating Firebase account and registering role..."
            val user = firebaseAuthService.signUp(name, email, password, role, phone, companyOrVehicle)
            _currentUser.value = user
            _currentRole.value = role

            // If registered as driver, register vehicle as well
            if (role == UserRole.VEHICLE_OWNER) {
                registerVehicle(
                    driver = name,
                    plate = "REG-" + (100..999).random(),
                    type = if (companyOrVehicle.isNotBlank()) companyOrVehicle else "Semi Truck",
                    capacity = 15000.0,
                    zone = "Country-wide"
                )
            }

            _syncMessage.value = "Account created! Welcome to ${when(role) {
                UserRole.CONSIGNEE -> "Shipper Portal"
                UserRole.VEHICLE_OWNER -> "Driver Portal"
                UserRole.ADMIN -> "Admin Command Center"
            }}"
            kotlinx.coroutines.delay(2000)
            _syncMessage.value = null
        }
    }

    fun signOut() {
        firebaseAuthService.signOut()
        _currentUser.value = null
        _syncMessage.value = "Signed out of session"
    }

    // Role switcher
    fun setRole(role: UserRole) {
        _currentRole.value = role
        _currentUser.value = demoAccounts[role] ?: _currentUser.value?.copy(role = role)
        // Auto-select vehicle when switching to Vehicle Owner
        if (role == UserRole.VEHICLE_OWNER && _selectedVehicleId.value == null) {
            viewModelScope.launch {
                vehicles.value.firstOrNull()?.let {
                    _selectedVehicleId.value = it.id
                }
            }
        }
    }

    // --- Synchronization & Debug Utilities ---

    fun synchronizeDebugData() {
        viewModelScope.launch {
            _syncMessage.value = "Synchronizing local database state..."
            resetDatabase()
            _syncMessage.value = "Debug state successfully synchronized with seed records!"
            kotlinx.coroutines.delay(3000)
            _syncMessage.value = null
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
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
            val insertedBidId = repository.insertBid(bid).toInt()

            // Initialize communication channel between driver & consignee immediately
            val shipment = shipments.value.find { it.id == shipmentId }
            val consigneeName = shipment?.senderName ?: "Shipper/Consignee"

            repository.insertChatMessage(
                com.example.data.model.ChatMessageEntity(
                    shipmentId = shipmentId,
                    bidId = insertedBidId,
                    senderRole = "SYSTEM",
                    senderName = "System Logistics Core",
                    receiverName = consigneeName,
                    message = "💬 Channel Initialized: Driver ${vehicle.driverName} placed a bid of $${String.format("%.2f", bidAmount)}.",
                    messageType = "SYSTEM"
                )
            )

            // Send introductory driver greeting
            repository.insertChatMessage(
                com.example.data.model.ChatMessageEntity(
                    shipmentId = shipmentId,
                    bidId = insertedBidId,
                    senderRole = "DRIVER",
                    senderName = vehicle.driverName,
                    receiverName = consigneeName,
                    message = "Hello! I placed a bid on your cargo (${shipment?.cargoType ?: "Shipment"}). Ready to discuss pickup details, gate instructions, and schedule.",
                    messageType = "TEXT"
                )
            )

            _syncMessage.value = "Bid placed! Communication channel created with Shipper."
        }
    }

    // 4. Accept Bid System (Consignee accepts carrier's bid)
    fun acceptBid(shipmentId: Int, bidId: Int) {
        viewModelScope.launch {
            repository.acceptBid(shipmentId, bidId)

            val shipment = shipments.value.find { it.id == shipmentId }
            val curUser = currentUser.value?.name ?: "Shipper"
            val driver = shipment?.driverName ?: "Fleet Driver"

            repository.insertChatMessage(
                com.example.data.model.ChatMessageEntity(
                    shipmentId = shipmentId,
                    bidId = bidId,
                    senderRole = "SYSTEM",
                    senderName = "System Logistics Core",
                    receiverName = driver,
                    message = "🎉 Bid Accepted! $curUser matched with $driver. Direct chat & call line locked in.",
                    messageType = "SYSTEM"
                )
            )

            _syncMessage.value = "Bid accepted! Driver notified & direct call line active."
        }
    }

    // --- Communication Channel (Chat & Call) Functions ---

    fun getMessagesForShipment(shipmentId: Int): Flow<List<com.example.data.model.ChatMessageEntity>> {
        return repository.getMessagesForShipment(shipmentId)
    }

    fun sendChatMessage(shipmentId: Int, messageText: String, receiverName: String) {
        val user = currentUser.value ?: return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            val roleStr = when (user.role) {
                UserRole.VEHICLE_OWNER -> "DRIVER"
                UserRole.CONSIGNEE -> "CONSIGNEE"
                UserRole.ADMIN -> "ADMIN"
            }

            repository.insertChatMessage(
                com.example.data.model.ChatMessageEntity(
                    shipmentId = shipmentId,
                    senderRole = roleStr,
                    senderName = user.name,
                    receiverName = receiverName,
                    message = messageText.trim(),
                    messageType = "TEXT"
                )
            )
        }
    }

    fun sendVoiceNote(shipmentId: Int, durationSec: Int, receiverName: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val roleStr = if (user.role == UserRole.VEHICLE_OWNER) "DRIVER" else "CONSIGNEE"
            repository.insertChatMessage(
                com.example.data.model.ChatMessageEntity(
                    shipmentId = shipmentId,
                    senderRole = roleStr,
                    senderName = user.name,
                    receiverName = receiverName,
                    message = "🎤 Voice note recorded (${durationSec}s)",
                    messageType = "VOICE_NOTE",
                    voiceNoteDurationSec = durationSec
                )
            )
        }
    }

    fun logCallActivity(shipmentId: Int, receiverName: String, durationStr: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val roleStr = if (user.role == UserRole.VEHICLE_OWNER) "DRIVER" else "CONSIGNEE"
            repository.insertChatMessage(
                com.example.data.model.ChatMessageEntity(
                    shipmentId = shipmentId,
                    senderRole = roleStr,
                    senderName = user.name,
                    receiverName = receiverName,
                    message = "📞 Voice Call ended ($durationStr)",
                    messageType = "CALL_LOG"
                )
            )
        }
    }

    fun markChatMessagesRead(shipmentId: Int) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.markChatMessagesRead(shipmentId, user.name)
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

    fun markShipmentArrived(shipmentId: Int) {
        viewModelScope.launch {
            val shipment = shipments.value.find { it.id == shipmentId } ?: return@launch
            val updated = shipment.copy(
                status = "ARRIVED",
                currentProgress = 0.98f,
                currentLat = shipment.dropoffLat,
                currentLng = shipment.dropoffLng
            )
            repository.updateShipment(updated)
            _syncMessage.value = "Carrier arrived at destination dock: ${shipment.dropoffLocation}"
            kotlinx.coroutines.delay(2000)
            _syncMessage.value = null
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

    // 9. Support Ticket Management
    fun submitSupportTicket(
        shipmentId: Int?,
        category: String,
        priority: String,
        description: String
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            val role = currentRole.value
            val ticket = com.example.data.model.SupportTicketEntity(
                userRole = role.name,
                userName = user?.name ?: "Logistics User",
                userEmail = user?.email ?: "user@logistics.com",
                userPhone = user?.phone ?: "+1 (555) 019-2834",
                shipmentId = shipmentId,
                issueCategory = category,
                priority = priority,
                description = description,
                status = "OPEN"
            )
            repository.insertSupportTicket(ticket)
            _syncMessage.value = "Support problem report submitted successfully! Dispatch agent notified."
            kotlinx.coroutines.delay(2500)
            _syncMessage.value = null
        }
    }

    fun respondToSupportTicket(ticketId: Int, status: String, response: String) {
        viewModelScope.launch {
            repository.respondToSupportTicket(ticketId, status, response)
            _syncMessage.value = "Support ticket updated to status: $status"
            kotlinx.coroutines.delay(2000)
            _syncMessage.value = null
        }
    }

    // --- Local Notification & Milestone Observer Engine ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "shipper_milestone_channel"
            val channelName = "Shipper Milestone Notifications"
            val descriptionText = "Alerts shippers when a delivery milestone status changes"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupShipmentMilestoneObserver() {
        viewModelScope.launch {
            repository.allShipments.collect { shipmentList ->
                for (shipment in shipmentList) {
                    val oldStatus = previousStatusMap[shipment.id]
                    if (oldStatus != null && oldStatus != shipment.status) {
                        handleMilestoneChange(shipment, oldStatus, shipment.status)
                    }
                    previousStatusMap[shipment.id] = shipment.status
                }
            }
        }
    }

    private fun handleMilestoneChange(shipment: ShipmentEntity, oldStatus: String, newStatus: String) {
        val readableOld = formatStatusLabel(oldStatus)
        val readableNew = formatStatusLabel(newStatus)
        val msg = "Shipment #${shipment.id} (${shipment.cargoType}) milestone changed: $readableOld ➔ $readableNew"

        val alert = MilestoneAlert(
            shipmentId = shipment.id,
            cargoType = shipment.cargoType,
            oldStatus = oldStatus,
            newStatus = newStatus,
            message = msg
        )

        _milestoneAlerts.value = listOf(alert) + _milestoneAlerts.value
        _syncMessage.value = "🔔 Shipper Alert: Cargo [${shipment.cargoType}] is now $readableNew"

        postLocalSystemNotification(
            shipmentId = shipment.id,
            cargoType = shipment.cargoType,
            oldStatus = readableOld,
            newStatus = readableNew
        )
    }

    private fun postLocalSystemNotification(
        shipmentId: Int,
        cargoType: String,
        oldStatus: String,
        newStatus: String
    ) {
        val context = getApplication<Application>()
        val channelId = "shipper_milestone_channel"
        val title = "🚚 Milestone Update: $cargoType"
        val text = "Shipment #$shipmentId moved from $oldStatus to $newStatus"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text.\nTap to view live tracking and delivery details."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 100, 250))

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (shipmentId * 1000) + (System.currentTimeMillis() % 1000).toInt()
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatStatusLabel(status: String): String {
        return when (status) {
            "PENDING_BIDS" -> "Pending Bids"
            "MATCHED" -> "Pickup Confirmed"
            "IN_TRANSIT" -> "In Transit"
            "ARRIVED" -> "Arrived at Dock"
            "DELIVERED" -> "Delivered & Signed"
            else -> status
        }
    }

    fun markAlertAsRead(alertId: String) {
        _milestoneAlerts.value = _milestoneAlerts.value.map {
            if (it.id == alertId) it.copy(isRead = true) else it
        }
    }

    fun clearAllMilestoneAlerts() {
        _milestoneAlerts.value = emptyList()
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
