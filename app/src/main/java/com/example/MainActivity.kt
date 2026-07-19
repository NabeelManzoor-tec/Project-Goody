package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.repository.LogisticsRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Initialize Room DB & Repository
                val context = androidx.compose.ui.platform.LocalContext.current
                val database = remember { AppDatabase.getDatabase(context.applicationContext) }
                val repository = remember { LogisticsRepository(database.logisticsDao()) }
                
                // Retrieve ViewModel
                val viewModel: LogisticsViewModel = viewModel(
                    factory = LogisticsViewModelFactory(
                        application = application,
                        repository = repository
                    )
                )

                val currentScreen by viewModel.currentScreen.collectAsState()
                val currentRole by viewModel.currentRole.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "Logistics Hub",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Zone: City, Highway, & SEZs",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            actions = {
                                // Dynamic inline Quick-Switcher for the "Three Ends"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "Role:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    
                                    var showRoleDropdown by remember { mutableStateOf(false) }
                                    AssistChip(
                                        onClick = { showRoleDropdown = true },
                                        label = {
                                            Text(
                                                text = when (currentRole) {
                                                    UserRole.ADMIN -> "Admin Controls"
                                                    UserRole.VEHICLE_OWNER -> "Vehicle Owner"
                                                    UserRole.CONSIGNEE -> "Consignee Portal"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = when (currentRole) {
                                                    UserRole.ADMIN -> Icons.Filled.SupervisorAccount
                                                    UserRole.VEHICLE_OWNER -> Icons.Filled.LocalShipping
                                                    UserRole.CONSIGNEE -> Icons.Filled.Person
                                                },
                                                contentDescription = "Role",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.testTag("appbar_role_chip")
                                    )

                                    DropdownMenu(
                                        expanded = showRoleDropdown,
                                        onDismissRequest = { showRoleDropdown = false }
                                    ) {
                                        UserRole.values().forEach { roleOption ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = when (roleOption) {
                                                            UserRole.ADMIN -> "Admin Controls"
                                                            UserRole.VEHICLE_OWNER -> "Vehicle Owner (Carrier)"
                                                            UserRole.CONSIGNEE -> "Consignee (Shipper)"
                                                        },
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.setRole(roleOption)
                                                    showRoleDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            // Bottom Nav Menubar
                            NavigationBarItem(
                                selected = currentScreen == Screen.DASHBOARD,
                                onClick = { viewModel.setScreen(Screen.DASHBOARD) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == Screen.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                        contentDescription = "Dashboard"
                                    )
                                },
                                label = { Text("Dashboard", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_dashboard")
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.ACTIVE_SHIPMENTS,
                                onClick = { viewModel.setScreen(Screen.ACTIVE_SHIPMENTS) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == Screen.ACTIVE_SHIPMENTS) Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping,
                                        contentDescription = "Active Shipments"
                                    )
                                },
                                label = { Text("Active Ledg.", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_active_shipments")
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.BILLING,
                                onClick = { viewModel.setScreen(Screen.BILLING) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == Screen.BILLING) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = "Billing"
                                    )
                                },
                                label = { Text("Billing Audit", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_billing")
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.SETTINGS,
                                onClick = { viewModel.setScreen(Screen.SETTINGS) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == Screen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text("Settings", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    // Multi-view router
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            Screen.ACTIVE_SHIPMENTS -> ActiveShipmentsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            Screen.BILLING -> BillingScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

