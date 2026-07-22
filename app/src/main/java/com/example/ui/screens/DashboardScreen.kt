package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import androidx.compose.runtime.collectAsState
import com.example.data.model.BidEntity
import com.example.data.model.ShipmentEntity
import kotlinx.coroutines.flow.Flow
import com.example.data.model.VehicleEntity
import com.example.ui.viewmodel.LogisticsViewModel
import com.example.ui.viewmodel.UserRole
import com.example.ui.viewmodel.Screen
import java.text.NumberFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: LogisticsViewModel,
    modifier: Modifier = Modifier
) {
    val role by viewModel.currentRole.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val shipments by viewModel.shipments.collectAsState()
    val bids by viewModel.bids.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val selectedVehicleId by viewModel.selectedVehicleId.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Hero Image Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_logistics_hero),
                    contentDescription = "Logistics Hero Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = when (role) {
                            UserRole.CONSIGNEE -> "Shipper Freight Portal"
                            UserRole.VEHICLE_OWNER -> "Driver & Carrier Portal"
                            UserRole.ADMIN -> "Admin Command Center"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Active Account: ${currentUser?.name ?: "Guest"} (${currentUser?.companyOrVehicle ?: "Logistics"})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // Dashboard content depends on selected user role
        when (role) {
            UserRole.CONSIGNEE -> {
                item { ConsigneeStatsSection(shipments, bids) }
                item { CreateShipmentCard(viewModel) }
                item { ConsigneeShipmentsHeader() }
                val myShipments = shipments.filter { it.senderName == "Consignee Portal" || it.senderName == "Fresh Farms Organic" || it.senderName == "Apex Hardware Corp" || it.senderName == "EcoTech Industries" || it.senderName == currentUser?.name }
                if (myShipments.isEmpty()) {
                    item { EmptyStateIndicator("No bookings found. Book your first freight load above.") }
                } else {
                    items(myShipments) { shipment ->
                        ShipmentDashboardItem(shipment, viewModel)
                    }
                }
            }
            UserRole.VEHICLE_OWNER -> {
                item { VehicleOwnerStatsSection(shipments, bids, selectedVehicleId) }
                item { ActiveCarrierSelector(vehicles, selectedVehicleId, viewModel) }
                item { AvailableLoadsHeader() }
                
                val currentVehicle = vehicles.find { it.id == selectedVehicleId }
                if (currentVehicle == null) {
                    item { EmptyStateIndicator("Please register or select a carrier vehicle in Settings.") }
                } else {
                    val availableLoads = shipments.filter { 
                        it.status == "PENDING_BIDS" && 
                        it.zone == currentVehicle.operatingZone &&
                        it.weightKg <= currentVehicle.capacityKg
                    }

                    if (availableLoads.isEmpty()) {
                        item { EmptyStateIndicator("No open loads match your vehicle's zone (${currentVehicle.operatingZone}) and capacity (${currentVehicle.capacityKg} kg).") }
                    } else {
                        items(availableLoads) { shipment ->
                            AvailableLoadDashboardItem(shipment, currentVehicle, bids, viewModel)
                        }
                    }
                }
            }
            UserRole.ADMIN -> {
                item { AdminStatsSection(shipments, vehicles, bids) }
                item { AdminDebugSyncCard(viewModel) }
                item { SystemParametersConfigCard() }
                item { GlobalShipmentsHeader() }
                if (shipments.isEmpty()) {
                    item { EmptyStateIndicator("No shipments in the system database.") }
                } else {
                    items(shipments) { shipment ->
                        AdminShipmentItem(shipment, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDebugSyncCard(viewModel: LogisticsViewModel) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = "Sync Debug",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Synchronize & Debug Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Resync local database state across Shipper, Driver, and Admin portals, trigger matching engine, or simulate GPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.synchronizeDebugData() },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("admin_sync_data_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Seed Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val pendingList = viewModel.shipments.value.filter { it.status == "PENDING_BIDS" }
                        pendingList.forEach { viewModel.runAutomatedMatching(it.id) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("admin_auto_match_all_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Match Loads", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- Consignee Composables ---

@Composable
fun ConsigneeStatsSection(shipments: List<ShipmentEntity>, bids: List<BidEntity>) {
    val activeCount = shipments.count { it.status == "MATCHED" || it.status == "IN_TRANSIT" }
    val pendingCount = shipments.count { it.status == "PENDING_BIDS" }
    val totalSpend = shipments.mapNotNull { it.acceptedBidPrice ?: it.basePrice }.sum()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Active Shipments",
            value = activeCount.toString(),
            icon = Icons.Filled.LocalShipping,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Awaiting Match",
            value = pendingCount.toString(),
            icon = Icons.Filled.AccessTime,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Total Value",
            value = formatCurrency(totalSpend),
            icon = Icons.Filled.MonetizationOn,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShipmentCard(viewModel: LogisticsViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var cargoType by remember { mutableStateOf("Fresh Berries") }
    var weightStr by remember { mutableStateOf("1200") }
    var pickupLoc by remember { mutableStateOf("Green Valley Farm") }
    var dropoffLoc by remember { mutableStateOf("Downtown Grocery Hub") }
    var zoneSelection by remember { mutableStateOf("City-wide") }
    var basePriceStr by remember { mutableStateOf("350") }
    var autoMatch by remember { mutableStateOf(false) }

    val zones = listOf("City-wide", "Country-wide", "Special Economy Zone")

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AddLocationAlt,
                        contentDescription = "Book load",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Book New Shipment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Register cargos, choose zone & match carriers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand form"
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider()

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = cargoType,
                            onValueChange = { cargoType = it },
                            label = { Text("Cargo Type") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = { weightStr = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = pickupLoc,
                        onValueChange = { pickupLoc = it },
                        label = { Text("Pickup Location Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dropoffLoc,
                        onValueChange = { dropoffLoc = it },
                        label = { Text("Destination Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Column {
                        Text(
                            text = "Zone Type Selection",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            zones.forEach { zone ->
                                FilterChip(
                                    selected = (zoneSelection == zone),
                                    onClick = { zoneSelection = zone },
                                    label = { Text(zone, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = basePriceStr,
                            onValueChange = { basePriceStr = it },
                            label = { Text("Target Base Budget ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Automated Match", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Assign instantly", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = autoMatch,
                                onCheckedChange = { autoMatch = it },
                                modifier = Modifier.testTag("automatch_switch")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val weight = weightStr.toDoubleOrNull() ?: 1000.0
                            val price = basePriceStr.toDoubleOrNull() ?: 300.0
                            viewModel.createShipment(
                                sender = "Consignee Portal",
                                receiver = "Destination Client",
                                pickup = pickupLoc,
                                dropoff = dropoffLoc,
                                cargo = cargoType,
                                weight = weight,
                                zone = zoneSelection,
                                basePrice = price,
                                autoMatch = autoMatch
                            )
                            expanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_shipment_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Submit")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Cargo & Post Load", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConsigneeShipmentsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "My Booked Shipments",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Track status & bids",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ShipmentDashboardItem(shipment: ShipmentEntity, viewModel: LogisticsViewModel) {
    val shipmentBids by viewModel.getBidsForShipment(shipment.id).collectAsState(initial = emptyList())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.selectShipment(shipment.id)
                viewModel.setScreen(Screen.ACTIVE_SHIPMENTS)
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = shipment.cargoType,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Weight: ${shipment.weightKg} kg • ${shipment.zone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(shipment.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = "Route",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${shipment.pickupLocation} ➔ ${shipment.dropoffLocation}",
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Budget Fare", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(shipment.basePrice),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (shipment.driverName != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Carrier Assigned", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = shipment.driverName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalOffer,
                            contentDescription = "Bids",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (shipmentBids.isEmpty()) "Bidding active..." else "${shipmentBids.size} offer(s) pending",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Direct Accept Bidding for Consignee viewable list
            if (shipment.status == "PENDING_BIDS" && shipmentBids.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Active Carrier Offers",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    shipmentBids.forEach { bid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(onClick = {}) // Consumes click to prevent navigating to Active Shipments screen
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bid.driverName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${bid.vehicleType} • Plate: ${bid.plateNumber}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = formatCurrency(bid.bidAmount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Button(
                                    onClick = { viewModel.acceptBid(shipment.id, bid.id) },
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("accept_bid_dashboard_${bid.id}"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Accept", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Carrier Composables ---

@Composable
fun VehicleOwnerStatsSection(
    shipments: List<ShipmentEntity>,
    bids: List<BidEntity>,
    selectedVehicleId: Int?
) {
    val myBids = bids.filter { it.vehicleId == selectedVehicleId }
    val acceptedBids = myBids.filter { it.status == "ACCEPTED" }
    val totalEarnings = acceptedBids.sumOf { it.bidAmount }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "My Posted Bids",
            value = myBids.size.toString(),
            icon = Icons.Filled.LocalOffer,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Active Loads",
            value = shipments.count { it.vehicleId == selectedVehicleId && it.status == "IN_TRANSIT" }.toString(),
            icon = Icons.Filled.LocalShipping,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Total Earnings",
            value = formatCurrency(totalEarnings),
            icon = Icons.Filled.Payments,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
fun ActiveCarrierSelector(
    vehicles: List<VehicleEntity>,
    selectedId: Int?,
    viewModel: LogisticsViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val currentVehicle = vehicles.find { it.id == selectedId }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "My Active Carrier Vehicle",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = "Truck",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (currentVehicle != null) {
                        Column {
                            Text(
                                text = "${currentVehicle.vehicleType} [${currentVehicle.plateNumber}]",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Zone: ${currentVehicle.operatingZone} • Capacity: ${currentVehicle.capacityKg} kg",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text("No vehicles registered. Add one in Settings tab.")
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                vehicles.forEach { vehicle ->
                    DropdownMenuItem(
                        text = {
                            Text("${vehicle.vehicleType} (${vehicle.plateNumber}) - ${vehicle.operatingZone}")
                        },
                        onClick = {
                            viewModel.selectVehicle(vehicle.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AvailableLoadsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Matching Open Cargo Loads",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Bid on freight loads",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AvailableLoadDashboardItem(
    shipment: ShipmentEntity,
    vehicle: VehicleEntity,
    bids: List<BidEntity>,
    viewModel: LogisticsViewModel
) {
    val myBidOnThis = bids.find { it.shipmentId == shipment.id && it.vehicleId == vehicle.id }
    var showBidInput by remember { mutableStateOf(false) }
    var bidAmountStr by remember { mutableStateOf((shipment.basePrice * 0.95).toInt().toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = shipment.cargoType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Weight: ${shipment.weightKg} kg | Target Budget: ${formatCurrency(shipment.basePrice)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(shipment.status)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = "Pin",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${shipment.pickupLocation} ➔ ${shipment.dropoffLocation}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            if (myBidOnThis != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Bid placed",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Your Bid: ${formatCurrency(myBidOnThis.bidAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Status: ${myBidOnThis.status}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                if (!showBidInput) {
                    Button(
                        onClick = { showBidInput = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("submit_bid_prompt_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.LocalOffer, contentDescription = "Bid", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Place Freight Bid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bidAmountStr,
                            onValueChange = { bidAmountStr = it },
                            label = { Text("Bid ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val amt = bidAmountStr.toDoubleOrNull() ?: (shipment.basePrice * 0.95)
                                viewModel.placeBid(shipment.id, vehicle.id, amt)
                                showBidInput = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("confirm_bid_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Confirm", fontSize = 12.sp)
                        }

                        IconButton(onClick = { showBidInput = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    }
                }
            }
        }
    }
}

// --- Admin Composables ---

@Composable
fun AdminStatsSection(
    shipments: List<ShipmentEntity>,
    vehicles: List<VehicleEntity>,
    bids: List<BidEntity>
) {
    val totalShipments = shipments.size
    val matchedCount = shipments.count { it.status != "PENDING_BIDS" }
    val matchingRate = if (totalShipments > 0) (matchedCount.toFloat() / totalShipments * 100).toInt() else 0
    val totalBidsPlaced = bids.size

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Global Loads",
            value = totalShipments.toString(),
            icon = Icons.Filled.Dashboard,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Carrier Match Rate",
            value = "$matchingRate%",
            icon = Icons.Outlined.Analytics,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Bids Submitted",
            value = totalBidsPlaced.toString(),
            icon = Icons.Filled.LocalOffer,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
fun SystemParametersConfigCard() {
    var baseRateMultiplier by remember { mutableStateOf(1.0f) }
    var matchingAlgorithm by remember { mutableStateOf("Nearest Carrier Priority") }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = "Parameters", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "System Logistics Controls (Admin)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Zone Base Fare Multiplier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${"%.2f".format(baseRateMultiplier)}x", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = baseRateMultiplier,
                    onValueChange = { baseRateMultiplier = it },
                    valueRange = 0.5f..2.5f,
                    modifier = Modifier.testTag("admin_multiplier_slider")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto Load Match Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(matchingAlgorithm, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = {
                        matchingAlgorithm = if (matchingAlgorithm == "Nearest Carrier Priority") "Dynamic Bidding Balance" else "Nearest Carrier Priority"
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Toggle Mode", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun GlobalShipmentsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Global Ledger System Shipments",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Supervise operations",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AdminShipmentItem(shipment: ShipmentEntity, viewModel: LogisticsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = shipment.cargoType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Sender: ${shipment.senderName} • ${shipment.zone}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(shipment.status)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = "Pin",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${shipment.pickupLocation} ➔ ${shipment.dropoffLocation}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("System Budget Base Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(shipment.basePrice),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            viewModel.selectShipment(shipment.id)
                            viewModel.setScreen(Screen.ACTIVE_SHIPMENTS)
                        }
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = { viewModel.deleteShipment(shipment) }
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// --- Common UI Reusables ---

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(105.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, containerColor, textColor) = when (status) {
        "PENDING_BIDS" -> Triple("Bidding Open", Color(0xFFFFF3E0), Color(0xFFE65100))
        "MATCHED" -> Triple("Carrier Matched", Color(0xFFE8F5E9), Color(0xFF1B5E20))
        "IN_TRANSIT" -> Triple("In Transit", Color(0xFFE3F2FD), Color(0xFF0D47A1))
        "DELIVERED" -> Triple("Delivered", Color(0xFFF3E5F5), Color(0xFF4A148C))
        else -> Triple(status, Color(0xFFECEFF1), Color(0xFF263238))
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun EmptyStateIndicator(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalShipping,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = msg,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}
