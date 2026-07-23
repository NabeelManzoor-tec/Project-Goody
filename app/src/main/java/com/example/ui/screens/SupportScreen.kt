package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShipmentEntity
import com.example.data.model.SupportTicketEntity
import com.example.ui.viewmodel.LogisticsViewModel
import com.example.ui.viewmodel.UserRole
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    viewModel: LogisticsViewModel,
    modifier: Modifier = Modifier
) {
    val role by viewModel.currentRole.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val shipments by viewModel.shipments.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()

    val context = LocalContext.current

    // Form state
    var selectedShipmentId by remember { mutableStateOf<Int?>(null) }
    var selectedCategory by remember { mutableStateOf("Delivery Delay") }
    var selectedPriority by remember { mutableStateOf("MEDIUM") }
    var descriptionText by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Admin respond dialog state
    var respondingTicket by remember { mutableStateOf<SupportTicketEntity?>(null) }
    var adminResponseText by remember { mutableStateOf("") }
    var adminTargetStatus by remember { mutableStateOf("IN_PROGRESS") }

    // Filter for tickets
    var ticketFilter by remember { mutableStateOf("ALL") }

    val userTickets = remember(supportTickets, role, currentUser, ticketFilter) {
        val baseList = if (role == UserRole.ADMIN) {
            supportTickets
        } else {
            supportTickets.filter { it.userEmail == currentUser?.email || it.userName.contains(currentUser?.name ?: "") }
        }
        when (ticketFilter) {
            "OPEN" -> baseList.filter { it.status == "OPEN" || it.status == "IN_PROGRESS" }
            "CRITICAL" -> baseList.filter { it.priority == "CRITICAL" || it.priority == "HIGH" }
            "RESOLVED" -> baseList.filter { it.status == "RESOLVED" || it.status == "CLOSED" }
            else -> baseList
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.HeadsetMic,
                                    contentDescription = "Support",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "24/7 Live Dispatch Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Text(
                        text = "Connect to Support & Emergency Help",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (role == UserRole.VEHICLE_OWNER)
                            "Driver Support: Report delays, roadside breakdowns, route issues, or contact dispatch immediately during transit."
                        else if (role == UserRole.CONSIGNEE)
                            "Shipper Support: Track package delays, request temperature checks, change dropoff notes, or resolve billing issues."
                        else
                            "Admin Emergency Center: Monitor all incoming driver and shipper tickets, post dispatch resolutions, and handle emergencies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Direct Hotline Quick Call Buttons
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Direct Immediate Hotlines",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+18005554357"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dispatch (800) 555-HELP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+18005557623"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Sos, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Roadside Emergency", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Submit Problem Report Form (For Drivers and Shippers, or Admin test)
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Report a Problem / Request Help",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = when (role) {
                                        UserRole.CONSIGNEE -> "Shipper Issue"
                                        UserRole.VEHICLE_OWNER -> "Driver On-Transit"
                                        UserRole.ADMIN -> "Admin Mode"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (role == UserRole.VEHICLE_OWNER) Icons.Filled.LocalShipping else Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }

                    HorizontalDivider()

                    // Quick Problem Presets depending on role
                    Text(
                        text = "Quick Problem Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val presets = if (role == UserRole.VEHICLE_OWNER) {
                        listOf(
                            "Heavy Traffic Delay" to "Delivery Delay",
                            "Vehicle Mechanical Issue" to "Vehicle Mechanical",
                            "Consignee Unreachable" to "Unreachable Recipient",
                            "Route / Dock Access Problem" to "Route & Address Help"
                        )
                    } else {
                        listOf(
                            "Delayed ETA Query" to "Delivery Delay",
                            "Temperature / Cargo Inspection" to "Cargo Damage",
                            "Change Dropoff Instructions" to "Route & Address Help",
                            "Billing / Rate Dispute" to "Billing / Dispute"
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { (presetName, categoryKey) ->
                            FilterChip(
                                selected = descriptionText.contains(presetName) || selectedCategory == categoryKey,
                                onClick = {
                                    selectedCategory = categoryKey
                                    if (!descriptionText.contains(presetName)) {
                                        descriptionText = if (descriptionText.isBlank()) presetName else "$descriptionText. $presetName"
                                    }
                                },
                                label = { Text(presetName, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }

                    // Select Linked Active Shipment
                    var shipmentDropdownExpanded by remember { mutableStateOf(false) }
                    val activeShipmentsList = remember(shipments, role, currentUser) {
                        if (role == UserRole.VEHICLE_OWNER) {
                            shipments.filter { it.driverName?.contains(currentUser?.name ?: "") == true || it.status == "IN_TRANSIT" || it.status == "MATCHED" }
                        } else {
                            shipments.filter { it.senderName == currentUser?.name || it.status != "DELIVERED" }
                        }
                    }

                    val selectedShipmentObj = shipments.find { it.id == selectedShipmentId }

                    ExposedDropdownMenuBox(
                        expanded = shipmentDropdownExpanded,
                        onExpandedChange = { shipmentDropdownExpanded = !shipmentDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (selectedShipmentObj != null)
                                "Shipment #${selectedShipmentObj.id}: ${selectedShipmentObj.cargoType} (${selectedShipmentObj.pickupLocation} -> ${selectedShipmentObj.dropoffLocation})"
                            else
                                "General Request / Unlinked to Shipment",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Link Active Delivery / Cargo Load") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shipmentDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = shipmentDropdownExpanded,
                            onDismissRequest = { shipmentDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("General Request / None") },
                                onClick = {
                                    selectedShipmentId = null
                                    shipmentDropdownExpanded = false
                                }
                            )
                            HorizontalDivider()
                            activeShipmentsList.forEach { shipment ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Shipment #${shipment.id}: ${shipment.cargoType}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${shipment.pickupLocation} → ${shipment.dropoffLocation} (${shipment.status})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedShipmentId = shipment.id
                                        shipmentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Issue Category Dropdown
                    var categoryDropdownExpanded by remember { mutableStateOf(false) }
                    val categories = listOf(
                        "Delivery Delay",
                        "Cargo Damage",
                        "Vehicle Mechanical",
                        "Route & Address Help",
                        "Unreachable Recipient",
                        "Billing / Dispute",
                        "Other"
                    )

                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Issue Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Priority Selector Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Priority Level:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "LOW" to Color(0xFF2E7D32),
                                "MEDIUM" to Color(0xFF1976D2),
                                "HIGH" to Color(0xFFE65100),
                                "CRITICAL" to Color(0xFFC62828)
                            ).forEach { (priorityKey, color) ->
                                val isSelected = selectedPriority == priorityKey
                                Surface(
                                    onClick = { selectedPriority = priorityKey },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) color else color.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, color),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = priorityKey,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else color
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Detailed Description
                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = { Text("Describe the problem or request in detail...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5
                    )

                    // Submit Button
                    Button(
                        onClick = {
                            if (descriptionText.isNotBlank()) {
                                viewModel.submitSupportTicket(
                                    shipmentId = selectedShipmentId,
                                    category = selectedCategory,
                                    priority = selectedPriority,
                                    description = descriptionText
                                )
                                descriptionText = ""
                                selectedShipmentId = null
                                showSuccessDialog = true
                            }
                        },
                        enabled = descriptionText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_support_ticket_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Support Ticket to Dispatch",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Support Tickets History Section Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (role == UserRole.ADMIN) "All Support & Dispatch Tickets" else "My Support Tickets & History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${userTickets.size} Record(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Ticket Filter Row
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("ALL", "OPEN", "CRITICAL", "RESOLVED")) { fKey ->
                        FilterChip(
                            selected = ticketFilter == fKey,
                            onClick = { ticketFilter = fKey },
                            label = {
                                Text(
                                    text = when (fKey) {
                                        "ALL" -> "All Tickets"
                                        "OPEN" -> "Active / In-Progress"
                                        "CRITICAL" -> "High / Critical"
                                        "RESOLVED" -> "Resolved"
                                        else -> fKey
                                    },
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }
            }
        }

        // List of Tickets
        if (userTickets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No active support tickets found",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "If you encounter any issues during delivery, submit a report using the form above.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(userTickets, key = { it.id }) { ticket ->
                SupportTicketCard(
                    ticket = ticket,
                    role = role,
                    onRespond = {
                        respondingTicket = ticket
                        adminResponseText = ticket.supportResponse ?: ""
                        adminTargetStatus = if (ticket.status == "OPEN") "IN_PROGRESS" else "RESOLVED"
                    }
                )
            }
        }
    }

    // Success Confirmation Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(36.dp))
            },
            title = { Text("Support Ticket Dispatched", fontWeight = FontWeight.Bold) },
            text = {
                Text("Your support request has been logged in the 24/7 Operations System. A dispatch supervisor is reviewing your report and will respond shortly.")
            },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }) {
                    Text("OK, Understood")
                }
            }
        )
    }

    // Admin Response Dialog
    if (respondingTicket != null) {
        val ticket = respondingTicket!!
        AlertDialog(
            onDismissRequest = { respondingTicket = null },
            icon = {
                Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text("Respond to Ticket #${ticket.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "User: ${ticket.userName} (${ticket.userRole})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Issue: ${ticket.issueCategory} - ${ticket.description}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Text("Update Ticket Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = adminTargetStatus == "IN_PROGRESS",
                            onClick = { adminTargetStatus = "IN_PROGRESS" },
                            label = { Text("In Progress", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = adminTargetStatus == "RESOLVED",
                            onClick = { adminTargetStatus = "RESOLVED" },
                            label = { Text("Resolved", fontSize = 11.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = adminResponseText,
                        onValueChange = { adminResponseText = it },
                        label = { Text("Official Support Response / Resolution Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminResponseText.isNotBlank()) {
                            viewModel.respondToSupportTicket(
                                ticketId = ticket.id,
                                status = adminTargetStatus,
                                response = adminResponseText
                            )
                            respondingTicket = null
                        }
                    },
                    enabled = adminResponseText.isNotBlank()
                ) {
                    Text("Save Response & Update Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { respondingTicket = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SupportTicketCard(
    ticket: SupportTicketEntity,
    role: UserRole,
    onRespond: () -> Unit
) {
    val statusColor = when (ticket.status) {
        "OPEN" -> Color(0xFFE65100)
        "IN_PROGRESS" -> Color(0xFF1976D2)
        "RESOLVED" -> Color(0xFF2E7D32)
        else -> Color.Gray
    }

    val priorityColor = when (ticket.priority) {
        "CRITICAL" -> Color(0xFFC62828)
        "HIGH" -> Color(0xFFE65100)
        "MEDIUM" -> Color(0xFF1976D2)
        else -> Color(0xFF2E7D32)
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()) }
    val formattedDate = remember(ticket.timestamp) { dateFormat.format(Date(ticket.timestamp)) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = ticket.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${ticket.priority} PRIORITY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "Ticket #${ticket.id}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = ticket.issueCategory,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reported by: ${ticket.userName} (${ticket.userRole})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                if (ticket.shipmentId != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Shipment #${ticket.shipmentId}", fontSize = 10.sp) },
                        leadingIcon = { Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }

            // Official Support Response Section
            if (!ticket.supportResponse.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.SupportAgent,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dispatch Agent Response:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = ticket.supportResponse,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Admin Action Button
            if (role == UserRole.ADMIN) {
                Button(
                    onClick = onRespond,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (ticket.supportResponse == null) "Respond to Ticket" else "Edit Response & Status",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
