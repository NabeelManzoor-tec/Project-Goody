package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BidEntity
import com.example.data.model.ShipmentEntity
import com.example.ui.components.ChatAndCallDialog
import com.example.ui.components.VoiceCallDialog
import com.example.ui.viewmodel.LogisticsViewModel
import com.example.ui.viewmodel.UserRole
import kotlinx.coroutines.flow.Flow

@Composable
fun ActiveShipmentsScreen(
    viewModel: LogisticsViewModel,
    modifier: Modifier = Modifier
) {
    val shipments by viewModel.shipments.collectAsState()
    val selectedId by viewModel.selectedShipmentId.collectAsState()
    val role by viewModel.currentRole.collectAsState()

    var filterStatus by remember { mutableStateOf("ALL") } // "ALL", "PENDING_BIDS", "MATCHED", "IN_TRANSIT", "DELIVERED"
    val filteredShipments = remember(shipments, filterStatus) {
        if (filterStatus == "ALL") shipments else shipments.filter { it.status == filterStatus }
    }

    val selectedShipment = shipments.find { it.id == selectedId }

    var activeChatShipmentId by remember { mutableStateOf<Int?>(null) }
    var activeChatRecipientName by remember { mutableStateOf("") }
    var activeChatRecipientPhone by remember { mutableStateOf("") }
    var activeChatRecipientSubtext by remember { mutableStateOf("") }

    var activeCallShipmentId by remember { mutableStateOf<Int?>(null) }
    var activeCallRecipientName by remember { mutableStateOf("") }
    var activeCallRecipientPhone by remember { mutableStateOf("") }
    var activeCallRecipientSubtext by remember { mutableStateOf("") }

    if (activeChatShipmentId != null) {
        ChatAndCallDialog(
            shipmentId = activeChatShipmentId!!,
            recipientName = activeChatRecipientName,
            recipientPhone = activeChatRecipientPhone,
            recipientSubtext = activeChatRecipientSubtext,
            viewModel = viewModel,
            onDismiss = { activeChatShipmentId = null }
        )
    }

    if (activeCallShipmentId != null) {
        VoiceCallDialog(
            shipmentId = activeCallShipmentId!!,
            recipientName = activeCallRecipientName,
            recipientPhone = activeCallRecipientPhone,
            recipientSubtext = activeCallRecipientSubtext,
            viewModel = viewModel,
            onDismiss = { activeCallShipmentId = null }
        )
    }

    if (selectedShipment == null) {
        // --- MASTER LIST VIEW: Clean Full Width Ledger of Deliveries ---
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Deliveries Ledger",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select any delivery load to open focused driver view & milestone controls",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${filteredShipments.size} Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Status Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = getStatusIndex(filterStatus),
                edgePadding = 0.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[getStatusIndex(filterStatus)]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("ALL", "PENDING_BIDS", "MATCHED", "IN_TRANSIT", "DELIVERED").forEach { status ->
                    Tab(
                        selected = filterStatus == status,
                        onClick = { filterStatus = status },
                        text = {
                            Text(
                                text = getFilterLabel(status),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            // Shipments List
            if (filteredShipments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No shipments match this status filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredShipments) { shipment ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectShipment(shipment.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = shipment.cargoType,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    StatusBadge(shipment.status)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Route visual row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = shipment.pickupLocation,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = " ➔ ",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = shipment.dropoffLocation,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Weight: ${shipment.weightKg} kg | Zone: ${shipment.zone}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (shipment.acceptedBidPrice != null) "Agreed Rate: ${formatCurrency(shipment.acceptedBidPrice)}" else "Base Rate: ${formatCurrency(shipment.basePrice)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (shipment.acceptedBidPrice != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.selectShipment(shipment.id) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("focus_delivery_${shipment.id}")
                                    ) {
                                        Text("Focus Delivery ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- DEDICATED FOCUSED DELIVERY DETAIL SCREEN ---
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top Navigation & Delivery Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { viewModel.selectShipment(null) },
                            modifier = Modifier.testTag("back_to_active_ledger")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Active Deliveries List",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Focused Delivery #${selectedShipment.id}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(selectedShipment.status)
                            }
                            Text(
                                text = "${selectedShipment.cargoType} • ${selectedShipment.weightKg} kg",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick Support Button
                    OutlinedButton(
                        onClick = { viewModel.setScreen(com.example.ui.viewmodel.Screen.SUPPORT) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("focused_header_support")
                    ) {
                        Icon(Icons.Filled.HeadsetMic, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Support", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Focused Delivery Details Scroll View
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 1. Live GPS Route Tracking Map
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Map,
                                        contentDescription = "Map",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Real-Time GPS Location & Route",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                if (selectedShipment.status == "IN_TRANSIT") {
                                    Text(
                                        text = "In Transit (${(selectedShipment.currentProgress * 100).toInt()}% • ETA 18 mins)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Vector Custom Canvas Map Drawer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B)) // Deep slate-dark background
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            ) {
                                val pickupLabel = selectedShipment.pickupLocation
                                val dropoffLabel = selectedShipment.dropoffLocation
                                val progress = selectedShipment.currentProgress
                                val status = selectedShipment.status

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val padding = 44.dp.toPx()
                                    val width = size.width
                                    val height = size.height

                                    val pickupOffset = Offset(padding, height / 2)
                                    val dropoffOffset = Offset(width - padding, height / 2)
                                    val currentOffset = Offset(
                                        pickupOffset.x + progress * (dropoffOffset.x - pickupOffset.x),
                                        pickupOffset.y
                                    )

                                    // Dotted path line
                                    drawLine(
                                        color = Color(0xFF64748B),
                                        start = pickupOffset,
                                        end = dropoffOffset,
                                        strokeWidth = 3f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )

                                    // Progress trail line
                                    if (progress > 0.0f) {
                                        drawLine(
                                            color = Color(0xFF3B82F6),
                                            start = pickupOffset,
                                            end = currentOffset,
                                            strokeWidth = 5f
                                        )
                                    }

                                    // Pickup Point (Green pin)
                                    drawCircle(
                                        color = Color(0xFF10B981),
                                        radius = 16f,
                                        center = pickupOffset
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 6f,
                                        center = pickupOffset
                                    )

                                    // Dropoff Point (Red pin)
                                    drawCircle(
                                        color = Color(0xFFEF4444),
                                        radius = 16f,
                                        center = dropoffOffset
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 6f,
                                        center = dropoffOffset
                                    )

                                    // Active Carrier Truck Node (Blue)
                                    if (status == "IN_TRANSIT" || status == "MATCHED" || status == "ARRIVED" || status == "DELIVERED") {
                                        drawCircle(
                                            color = Color(0xFF3B82F6),
                                            radius = 22f,
                                            center = currentOffset
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 10f,
                                            center = currentOffset
                                        )
                                    }
                                }

                                Text(
                                    text = "Pickup: $pickupLabel",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 12.dp, top = 56.dp)
                                )

                                Text(
                                    text = "Dropoff: $dropoffLabel",
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 12.dp, top = 56.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Milestone Delivery Status Timeline (Pickup, Transit, Arrived, Delivered)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Milestone Delivery Status Timeline",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val currentStatus = selectedShipment.status
                            val progress = selectedShipment.currentProgress

                            val isPickupDone = currentStatus == "MATCHED" || currentStatus == "IN_TRANSIT" || currentStatus == "ARRIVED" || currentStatus == "DELIVERED"
                            val isTransitDone = currentStatus == "ARRIVED" || currentStatus == "DELIVERED" || (currentStatus == "IN_TRANSIT" && progress >= 0.85f)
                            val isArrivedDone = currentStatus == "ARRIVED" || currentStatus == "DELIVERED" || progress >= 0.98f
                            val isDeliveredDone = currentStatus == "DELIVERED"

                            // Horizontal Stage Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MilestoneBadgeStep("Pickup", isPickupDone, currentStatus == "MATCHED" || currentStatus == "PENDING_BIDS")
                                Text("➔", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MilestoneBadgeStep("Transit", isTransitDone, currentStatus == "IN_TRANSIT")
                                Text("➔", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MilestoneBadgeStep("Arrived", isArrivedDone, currentStatus == "ARRIVED" || (currentStatus == "IN_TRANSIT" && progress >= 0.9f))
                                Text("➔", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MilestoneBadgeStep("Delivered", isDeliveredDone, currentStatus == "DELIVERED")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            MilestoneItem(
                                title = "1. Pickup Stage",
                                description = if (isPickupDone) "Cargo successfully picked up at origin (${selectedShipment.pickupLocation})." else "Awaiting carrier dispatch & pickup at ${selectedShipment.pickupLocation}.",
                                isDone = isPickupDone,
                                isActive = currentStatus == "PENDING_BIDS" || currentStatus == "MATCHED"
                            )
                            MilestoneItem(
                                title = "2. Transit Stage",
                                description = if (currentStatus == "IN_TRANSIT") "Carrier actively on route. Live Progress: ${(progress * 100).toInt()}%" else if (isTransitDone) "Highway transit completed." else "Pending pickup departure.",
                                isDone = isTransitDone,
                                isActive = currentStatus == "IN_TRANSIT"
                            )
                            MilestoneItem(
                                title = "3. Arrived Stage",
                                description = if (isArrivedDone || currentStatus == "ARRIVED") "Vehicle arrived at dropoff dock (${selectedShipment.dropoffLocation}). Offloading in progress." else "Vehicle en route to destination dock.",
                                isDone = isArrivedDone,
                                isActive = currentStatus == "ARRIVED" || (currentStatus == "IN_TRANSIT" && progress >= 0.90f)
                            )
                            MilestoneItem(
                                title = "4. Delivered Stage",
                                description = if (isDeliveredDone) "Handed over to recipient (${selectedShipment.receiverName}). Proof of delivery signed." else "Awaiting final consignee inspection and delivery sign-off.",
                                isDone = isDeliveredDone,
                                isActive = currentStatus == "DELIVERED"
                            )
                        }
                    }
                }

                // 3. Driver Operational Focus Console
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Sim",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Driver Delivery Focus Panel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Driver actions to advance stages, send GPS pulses, and complete proof of delivery.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (selectedShipment.status == "MATCHED") {
                                    Button(
                                        onClick = { viewModel.startShipmentTransit(selectedShipment.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("start_transit_button")
                                    ) {
                                        Text("Confirm Pickup & Start Transit", fontSize = 11.sp)
                                    }
                                }

                                if (selectedShipment.status == "IN_TRANSIT") {
                                    Button(
                                        onClick = { viewModel.simulateTransitLocationUpdate(selectedShipment.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("gps_pulse_button")
                                    ) {
                                        Text("Pulse GPS", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { viewModel.markShipmentArrived(selectedShipment.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("mark_arrived_button")
                                    ) {
                                        Text("Mark Arrived", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.setScreen(com.example.ui.viewmodel.Screen.SUPPORT) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("shipment_support_button")
                                    ) {
                                        Icon(Icons.Filled.HeadsetMic, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Support", fontSize = 11.sp)
                                    }
                                }

                                if (selectedShipment.status == "ARRIVED") {
                                    Button(
                                        onClick = { viewModel.markShipmentDelivered(selectedShipment.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("complete_delivery_button")
                                    ) {
                                        Text("Complete Delivery & Sign", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.setScreen(com.example.ui.viewmodel.Screen.SUPPORT) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("shipment_support_button")
                                    ) {
                                        Icon(Icons.Filled.HeadsetMic, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Support", fontSize = 11.sp)
                                    }
                                }

                                if (selectedShipment.status != "MATCHED" && selectedShipment.status != "IN_TRANSIT" && selectedShipment.status != "ARRIVED") {
                                    Text(
                                        text = if (selectedShipment.status == "DELIVERED") "Shipment delivered & completed." else "Waiting for carrier matching/bidding to start transit.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Freight Specifications & Contact Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Freight & Contact Specifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Origin Sender", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(selectedShipment.senderName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(selectedShipment.pickupLocation, fontSize = 11.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Destination Consignee", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(selectedShipment.receiverName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(selectedShipment.dropoffLocation, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Cargo Weight: ${selectedShipment.weightKg} kg", fontSize = 11.sp)
                                    Text("Freight Zone: ${selectedShipment.zone}", fontSize = 11.sp)
                                }
                                Column {
                                    Text(
                                        text = if (selectedShipment.acceptedBidPrice != null) "Agreed Rate: ${formatCurrency(selectedShipment.acceptedBidPrice)}" else "Base Rate: ${formatCurrency(selectedShipment.basePrice)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (selectedShipment.driverName != null) {
                                        Text("Driver: ${selectedShipment.driverName}", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Direct Communication Channel
                            Text(
                                text = "Direct Communication Channel",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (role == UserRole.CONSIGNEE) "Discuss logistics & delivery instructions with ${selectedShipment.driverName ?: "Carrier"}" else "Negotiate fare & discuss pickup logistics with ${selectedShipment.senderName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        activeCallShipmentId = selectedShipment.id
                                        val otherParty = if (role == UserRole.CONSIGNEE) (selectedShipment.driverName ?: "Assigned Driver") else selectedShipment.senderName
                                        activeCallRecipientName = otherParty
                                        activeCallRecipientPhone = "+1 (555) 762-1082"
                                        activeCallRecipientSubtext = "Shipment #${selectedShipment.id} | ${selectedShipment.cargoType}"
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("focused_call_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Voice Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        activeChatShipmentId = selectedShipment.id
                                        val otherParty = if (role == UserRole.CONSIGNEE) (selectedShipment.driverName ?: "Assigned Driver") else selectedShipment.senderName
                                        activeChatRecipientName = otherParty
                                        activeChatRecipientPhone = "+1 (555) 762-1082"
                                        activeChatRecipientSubtext = "Shipment #${selectedShipment.id} | ${selectedShipment.cargoType}"
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("focused_chat_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 5. Shipper/Consignee Bidding Panel (Visible if PENDING_BIDS)
                if (selectedShipment.status == "PENDING_BIDS") {
                    item {
                        val shipmentBids by viewModel.getBidsForShipment(selectedShipment.id).collectAsState(initial = emptyList())

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Active Carrier Bids Submitted",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Carriers are offering these competitive freight rates. Accept to book instantly.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (shipmentBids.isEmpty()) {
                                    Text(
                                        text = "Awaiting first carrier rate bid. Bidding operates transparently.",
                                        fontSize = 12.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        shipmentBids.forEach { bid ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "${bid.driverName} • ${bid.vehicleType}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = "Plate: ${bid.plateNumber} | Amount: ${formatCurrency(bid.bidAmount)}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                if (role == UserRole.CONSIGNEE || role == UserRole.ADMIN) {
                                                    Button(
                                                        onClick = { viewModel.acceptBid(selectedShipment.id, bid.id) },
                                                        modifier = Modifier
                                                            .height(36.dp)
                                                            .testTag("accept_bid_${bid.id}"),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("Accept Bid", fontSize = 11.sp)
                                                    }
                                                } else {
                                                    Text(
                                                        text = bid.status,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MilestoneBadgeStep(
    label: String,
    isDone: Boolean,
    isActive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = if (isDone) Color(0xFF10B981) else if (isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal,
            color = if (isDone) Color(0xFF10B981) else if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MilestoneItem(
    title: String,
    description: String,
    isDone: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Icon(
                imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) Color(0xFF10B981) else if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(30.dp)
                    .background(
                        if (isDone) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isActive) MaterialTheme.colorScheme.primary else if (isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getStatusIndex(status: String): Int {
    return when (status) {
        "ALL" -> 0
        "PENDING_BIDS" -> 1
        "MATCHED" -> 2
        "IN_TRANSIT" -> 3
        "DELIVERED" -> 4
        else -> 0
    }
}

fun getFilterLabel(status: String): String {
    return when (status) {
        "ALL" -> "All"
        "PENDING_BIDS" -> "Bidding"
        "MATCHED" -> "Matched"
        "IN_TRANSIT" -> "Transit"
        "DELIVERED" -> "Arrived"
        else -> status
    }
}

