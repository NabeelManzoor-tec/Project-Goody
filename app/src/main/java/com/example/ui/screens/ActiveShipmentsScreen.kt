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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BidEntity
import com.example.data.model.ShipmentEntity
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
    val bids by viewModel.bids.collectAsState()
    val role by viewModel.currentRole.collectAsState()

    var filterStatus by remember { mutableStateOf("ALL") } // "ALL", "PENDING_BIDS", "MATCHED", "IN_TRANSIT", "DELIVERED"
    val filteredShipments = remember(shipments, filterStatus) {
        if (filterStatus == "ALL") shipments else shipments.filter { it.status == filterStatus }
    }

    val selectedShipment = shipments.find { it.id == selectedId }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Shipments Master List
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter Bar
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
                                fontSize = 11.sp,
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
                    Text(
                        text = "No shipments match this filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredShipments) { shipment ->
                        val isSelected = shipment.id == selectedId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectShipment(shipment.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            border = if (isSelected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = shipment.cargoType,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    StatusBadge(shipment.status)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${shipment.pickupLocation} ➔ ${shipment.dropoffLocation}",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Base Rate: ${formatCurrency(shipment.basePrice)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (shipment.acceptedBidPrice != null) {
                                        Text(
                                            text = "Final Rate: ${formatCurrency(shipment.acceptedBidPrice)}",
                                            fontSize = 11.sp,
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

        // Right Column: Shipment Details & Interactive Simulator Tracking Map
        Column(
            modifier = Modifier
                .weight(1.8f)
                .fillMaxHeight()
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedShipment == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Filled.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Select a shipment from the ledger to view real-time tracking, live bids, and simulation details.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Live Interactive Tracking Map Card
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
                                            text = "Simulated GPS Location Tracker",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    if (selectedShipment.status == "IN_TRANSIT") {
                                        Text(
                                            text = "In Transit (${(selectedShipment.currentProgress * 100).toInt()}% ETA 18 mins)",
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
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B)) // Deep slate-dark background
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                ) {
                                    val pickupLabel = selectedShipment.pickupLocation
                                    val dropoffLabel = selectedShipment.dropoffLocation
                                    val progress = selectedShipment.currentProgress
                                    val status = selectedShipment.status

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val padding = 40.dp.toPx()
                                        val width = size.width
                                        val height = size.height

                                        // Node coords
                                        val pickupOffset = Offset(padding, height / 2)
                                        val dropoffOffset = Offset(width - padding, height / 2)
                                        val currentOffset = Offset(
                                            pickupOffset.x + progress * (dropoffOffset.x - pickupOffset.x),
                                            pickupOffset.y
                                        )

                                        // Draw dotted path line
                                        drawLine(
                                            color = Color(0xFF64748B),
                                            start = pickupOffset,
                                            end = dropoffOffset,
                                            strokeWidth = 3f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )

                                        // Draw Completed progress line
                                        if (progress > 0.0f) {
                                            drawLine(
                                                color = Color(0xFF3B82F6), // Blue transit trail
                                                start = pickupOffset,
                                                end = currentOffset,
                                                strokeWidth = 5f
                                            )
                                        }

                                        // Draw Pickup Point (Green pin)
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

                                        // Draw Dropoff Point (Red pin)
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

                                        // Draw Active Carrier Truck Node (Blue) if MATCHED or IN_TRANSIT
                                        if (status == "IN_TRANSIT" || status == "MATCHED" || status == "DELIVERED") {
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

                                    // Graphical Labels overlay on Map
                                    Text(
                                        text = pickupLabel,
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(start = 12.dp, top = 48.dp)
                                    )

                                    Text(
                                        text = dropoffLabel,
                                        color = Color(0xFFEF4444),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 12.dp, top = 48.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Shipment Progress Timeline (Milestones)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Milestone Delivery Timeline",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val currentStatus = selectedShipment.status
                                val progress = selectedShipment.currentProgress

                                MilestoneItem(
                                    title = "Cargo Load Registered",
                                    description = "Shipment posted in the zone by sender.",
                                    isDone = true,
                                    isActive = currentStatus == "PENDING_BIDS"
                                )
                                MilestoneItem(
                                    title = "Carrier Load Matched",
                                    description = selectedShipment.driverName?.let { "Assigned driver: $it" } ?: "Awaiting automatic match or bid acceptance.",
                                    isDone = currentStatus != "PENDING_BIDS",
                                    isActive = currentStatus == "MATCHED"
                                )
                                MilestoneItem(
                                    title = "Freight In Transit",
                                    description = if (currentStatus == "IN_TRANSIT") "Driver tracking is live. Progress: ${(progress*100).toInt()}%" else "Cargo loaded onto matched carrier.",
                                    isDone = currentStatus == "IN_TRANSIT" || currentStatus == "DELIVERED",
                                    isActive = currentStatus == "IN_TRANSIT"
                                )
                                MilestoneItem(
                                    title = "Delivered & Signed",
                                    description = "Cargo arrived at destination.",
                                    isDone = currentStatus == "DELIVERED",
                                    isActive = currentStatus == "DELIVERED"
                                )
                            }
                        }
                    }

                    // 3. Driver Simulator Controls Console (Visible to carrier owners or admin)
                    if (role == UserRole.VEHICLE_OWNER || role == UserRole.ADMIN) {
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
                                            text = "Driver Simulator Control Panel",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "As the carrier, simulate physical hardware events like starting transit or pulsing location coordinates.",
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
                                                Text("Start Transit", fontSize = 11.sp)
                                            }
                                        }

                                        if (selectedShipment.status == "IN_TRANSIT") {
                                            Button(
                                                onClick = { viewModel.simulateTransitLocationUpdate(selectedShipment.id) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("gps_pulse_button")
                                            ) {
                                                Text("Pulse GPS Location", fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = { viewModel.markShipmentDelivered(selectedShipment.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("complete_delivery_button")
                                            ) {
                                                Text("Arrive Destination", fontSize = 11.sp)
                                            }
                                        }

                                        if (selectedShipment.status != "MATCHED" && selectedShipment.status != "IN_TRANSIT") {
                                            Text(
                                                text = "Carrier simulation is only active for matched/in-transit loads.",
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
                    }

                    // 4. Shipper/Consignee Bidding Panel (Visible to consignee creators to accept bids)
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
