package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BidEntity
import com.example.data.model.ShipmentEntity
import com.example.ui.viewmodel.LogisticsViewModel
import com.example.ui.viewmodel.UserRole

data class ActiveChannel(
    val shipment: ShipmentEntity,
    val recipientName: String,
    val recipientPhone: String,
    val recipientSubtext: String,
    val unreadCount: Int,
    val latestMessage: String
)

@Composable
fun CommunicationCenterDialog(
    viewModel: LogisticsViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val shipments by viewModel.shipments.collectAsState()
    val bids by viewModel.bids.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val allChatMessages by viewModel.allChatMessages.collectAsState()

    var activeChatShipmentId by remember { mutableStateOf<Int?>(null) }
    var activeChatRecipientName by remember { mutableStateOf("") }
    var activeChatRecipientPhone by remember { mutableStateOf("") }
    var activeChatRecipientSubtext by remember { mutableStateOf("") }

    var activeCallShipmentId by remember { mutableStateOf<Int?>(null) }
    var activeCallRecipientName by remember { mutableStateOf("") }
    var activeCallRecipientPhone by remember { mutableStateOf("") }
    var activeCallRecipientSubtext by remember { mutableStateOf("") }

    // Build channel list based on shipments that have bids or assigned drivers
    val channels = remember(shipments, bids, vehicles, allChatMessages, currentUser) {
        val userRole = currentUser?.role ?: UserRole.CONSIGNEE
        val curUserName = currentUser?.name ?: ""

        shipments.mapNotNull { shipment ->
            val shipmentBids = bids.filter { it.shipmentId == shipment.id }
            val shipmentMessages = allChatMessages.filter { it.shipmentId == shipment.id }

            if (shipmentBids.isEmpty() && shipmentMessages.isEmpty() && shipment.driverName == null) {
                return@mapNotNull null
            }

            val unreadCount = shipmentMessages.count { !it.isRead && it.senderName != curUserName }
            val latestMsgText = shipmentMessages.lastOrNull()?.message
                ?: if (shipment.acceptedBidPrice != null) "Bid accepted: $${shipment.acceptedBidPrice}"
                else "Active bidding channel"

            val recipientName: String
            val recipientPhone: String
            val recipientSubtext: String

            if (userRole == UserRole.VEHICLE_OWNER) {
                // Driver perspective -> Recipient is Shipper / Consignee
                recipientName = shipment.senderName
                recipientPhone = "+1 (555) 892-1001"
                recipientSubtext = "Shipper | Cargo: ${shipment.cargoType}"
            } else {
                // Consignee or Admin perspective -> Recipient is Driver
                if (shipment.driverName != null) {
                    recipientName = shipment.driverName
                    recipientPhone = shipment.driverPhone ?: "+1 (555) 019-2834"
                    recipientSubtext = "Driver | Cargo: ${shipment.cargoType}"
                } else if (shipmentBids.isNotEmpty()) {
                    val latestBid = shipmentBids.first()
                    recipientName = latestBid.driverName
                    recipientPhone = "+1 (555) 762-1082"
                    recipientSubtext = "${latestBid.vehicleType} (${latestBid.plateNumber}) | Bid: $${latestBid.bidAmount}"
                } else {
                    recipientName = "Fleet Driver"
                    recipientPhone = "+1 (555) 019-2834"
                    recipientSubtext = "Assigned Driver"
                }
            }

            ActiveChannel(
                shipment = shipment,
                recipientName = recipientName,
                recipientPhone = recipientPhone,
                recipientSubtext = recipientSubtext,
                unreadCount = unreadCount,
                latestMessage = latestMsgText
            )
        }
    }

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Communication Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Driver & Shipper Chat / Call Channels",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_communication_hub")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                if (channels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.QuestionAnswer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No active chat channels yet.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Place a bid on a shipment to start a direct channel.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(channels, key = { it.shipment.id }) { channel ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (channel.unreadCount > 0) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                ),
                                border = if (channel.unreadCount > 0) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocalShipping,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${channel.shipment.cargoType} (#${channel.shipment.id})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        if (channel.unreadCount > 0) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.error,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = "${channel.unreadCount} NEW",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onError,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "With: ${channel.recipientName} (${channel.recipientSubtext})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = channel.latestMessage,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedIconButton(
                                            onClick = {
                                                activeCallShipmentId = channel.shipment.id
                                                activeCallRecipientName = channel.recipientName
                                                activeCallRecipientPhone = channel.recipientPhone
                                                activeCallRecipientSubtext = channel.recipientSubtext
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("hub_call_btn_${channel.shipment.id}"),
                                            shape = CircleShape
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Phone,
                                                contentDescription = "Call",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                activeChatShipmentId = channel.shipment.id
                                                activeChatRecipientName = channel.recipientName
                                                activeChatRecipientPhone = channel.recipientPhone
                                                activeChatRecipientSubtext = channel.recipientSubtext
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("hub_chat_btn_${channel.shipment.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Chat,
                                                contentDescription = "Chat",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Chat & Discuss", fontSize = 11.sp)
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
