package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShipmentEntity
import com.example.ui.viewmodel.LogisticsViewModel
import com.example.ui.viewmodel.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: LogisticsViewModel,
    modifier: Modifier = Modifier
) {
    val shipments by viewModel.shipments.collectAsState()
    val role by viewModel.currentRole.collectAsState()

    var selectedInvoiceId by remember { mutableStateOf<Int?>(null) }
    val selectedShipment = shipments.find { it.id == selectedInvoiceId }

    // Aggregate values
    val totalRevenue = shipments.sumOf { it.acceptedBidPrice ?: it.basePrice }
    val platformFees = totalRevenue * 0.08 // 8% platform matching fee
    val paidCount = shipments.count { it.status == "DELIVERED" }
    val pendingCount = shipments.count { it.status != "DELIVERED" }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Financial Audit & Ledger",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Billing Summary Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Gross Value", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalRevenue), fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("All Zone Loads", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Platform Fee (8%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(platformFees), fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("Broker clearance", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1.1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Settled / Pending", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$paidCount / $pendingCount", fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("Deliveries metrics", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text(
                text = "Generated Zone Receipts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Invoices/Bills List
            if (shipments.isEmpty()) {
                EmptyStateIndicator("No billing records found in the database ledger.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(shipments) { shipment ->
                        val baseCost = shipment.basePrice
                        val bidCost = shipment.acceptedBidPrice
                        val finalCost = bidCost ?: baseCost
                        val platformTax = finalCost * 0.08
                        val carrierShare = finalCost - platformTax

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedInvoiceId = shipment.id }
                                .testTag("invoice_item_${shipment.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.ReceiptLong,
                                        contentDescription = "Invoice",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "INV-#10${shipment.id} [${shipment.cargoType}]",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${shipment.zone} • ${if (shipment.status == "DELIVERED") "Settled" else "Pending Deliver"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatCurrency(finalCost),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        color = if (shipment.status == "DELIVERED") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (shipment.status == "DELIVERED") "PAID" else "PENDING",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = if (shipment.status == "DELIVERED") Color(0xFF1B5E20) else Color(0xFFE65100)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Receipt Modal Dialog Sheet
        if (selectedShipment != null) {
            AlertDialog(
                onDismissRequest = { selectedInvoiceId = null },
                confirmButton = {
                    Button(
                        onClick = { selectedInvoiceId = null },
                        modifier = Modifier.testTag("dismiss_receipt_button")
                    ) {
                        Text("Close Ledger")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Security Receipt", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zone Security Invoicing Receipt", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    val baseCost = selectedShipment.basePrice
                    val finalCost = selectedShipment.acceptedBidPrice ?: baseCost
                    val brokerTax = finalCost * 0.08
                    val carrierPay = finalCost - brokerTax

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9)) // Classic soft gray receipt backing
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "OFFICIAL TRANSACTION MEMORANDUM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF475569),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Divider(color = Color(0xFF94A3B8))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Invoice Number:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("INV-10${selectedShipment.id}", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Assigned Zone:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(selectedShipment.zone, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cargo Type / Weight:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("${selectedShipment.cargoType} (${selectedShipment.weightKg} kg)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        Divider(color = Color(0xFFCBD5E1))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Brokerage Base Budget:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(formatCurrency(selectedShipment.basePrice), fontSize = 11.sp)
                        }

                        if (selectedShipment.acceptedBidPrice != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Contract Bid Freight:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(formatCurrency(selectedShipment.acceptedBidPrice), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Zone Match Surcharge (8%):", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("+ ${formatCurrency(brokerTax)}", fontSize = 11.sp, color = Color(0xFFD97706))
                        }

                        Divider(color = Color(0xFF94A3B8))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gross Settled Invoicing:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(formatCurrency(finalCost), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // Mock Barcode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "||||||  |||  |||||  ||  |||||  |||",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        }
                    }
                }
            )
        }
    }
}
