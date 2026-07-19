package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.LogisticsViewModel
import com.example.ui.viewmodel.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LogisticsViewModel,
    modifier: Modifier = Modifier
) {
    val role by viewModel.currentRole.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()

    var showAddVehicle by remember { mutableStateOf(false) }
    var driverName by remember { mutableStateOf("") }
    var plateNumber by remember { mutableStateOf("") }
    var capacityStr by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("Semi Truck") }
    var zoneSelection by remember { mutableStateOf("Country-wide") }

    val vehicleTypes = listOf("Box Truck", "Semi Truck", "Refrigerated Van", "Flatbed Trailer")
    val zones = listOf("City-wide", "Country-wide", "Special Economy Zone")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Text(
                text = "System Setup & Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // 1. Role Setup
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Simulation Active Identity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Switch between Admin controls, Truck Owners, or Consignees to fully test all automated features on one screen.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserRole.values().forEach { value ->
                            val selected = role == value
                            Button(
                                onClick = { viewModel.setRole(value) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("role_button_${value.name}"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (value) {
                                        UserRole.ADMIN -> "Admin"
                                        UserRole.VEHICLE_OWNER -> "Vehicle Owner"
                                        UserRole.CONSIGNEE -> "Consignee"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Vehicle Registration Manager (Add Carrier truck)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddVehicle = !showAddVehicle },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalShipping, contentDescription = "Add Truck", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Carrier Fleet Registration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(
                            imageVector = if (showAddVehicle) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }

                    AnimatedVisibility(
                        visible = showAddVehicle,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Divider()

                            OutlinedTextField(
                                value = driverName,
                                onValueChange = { driverName = it },
                                label = { Text("Driver Legal Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = plateNumber,
                                    onValueChange = { plateNumber = it },
                                    label = { Text("Plate Number") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = capacityStr,
                                    onValueChange = { capacityStr = it },
                                    label = { Text("Capacity (kg)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            // Vehicle Type Selector
                            Column {
                                Text("Carrier Vehicle Type", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    vehicleTypes.forEach { type ->
                                        FilterChip(
                                            selected = vehicleType == type,
                                            onClick = { vehicleType = type },
                                            label = { Text(type, fontSize = 9.sp) }
                                        )
                                    }
                                }
                            }

                            // Operating Zone
                            Column {
                                Text("Zone Area Authority", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    zones.forEach { zone ->
                                        FilterChip(
                                            selected = zoneSelection == zone,
                                            onClick = { zoneSelection = zone },
                                            label = { Text(zone, fontSize = 9.sp) }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (driverName.isNotBlank() && plateNumber.isNotBlank()) {
                                        val cap = capacityStr.toDoubleOrNull() ?: 5000.0
                                        viewModel.registerVehicle(driverName, plateNumber, vehicleType, cap, zoneSelection)
                                        driverName = ""
                                        plateNumber = ""
                                        capacityStr = ""
                                        showAddVehicle = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_register_vehicle_button")
                            ) {
                                Text("Enroll Vehicle into Carrier Network")
                            }
                        }
                    }
                }
            }
        }

        // 3. Registered Carrier fleet summary list
        item {
            Text("Registered Carrier Fleet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (vehicles.isEmpty()) {
            item { Text("No vehicles registered in the carrier pool.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(vehicles) { vehicle ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalShipping, contentDescription = "Truck", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("${vehicle.driverName} (${vehicle.plateNumber})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Type: ${vehicle.vehicleType} | Capacity: ${vehicle.capacityKg} kg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Zone: ${vehicle.operatingZone}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Surface(
                            color = if (vehicle.isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (vehicle.isAvailable) "Available" else "Busy",
                                color = if (vehicle.isAvailable) Color(0xFF1B5E20) else Color(0xFFC62828),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Debugging & Reset Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = "Reset", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Prototype Security Reset", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        text = "Resetting the system database purges custom transactions and re-seeds the schema with the default carrier fleets, bids, and shipments.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { viewModel.resetDatabase() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_database_button")
                    ) {
                        Text("Reset System & Seed Default Data", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
