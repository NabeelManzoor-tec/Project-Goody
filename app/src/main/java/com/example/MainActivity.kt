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
                val currentUser by viewModel.currentUser.collectAsState()
                val syncMessage by viewModel.syncMessage.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(syncMessage) {
                    syncMessage?.let {
                        snackbarHostState.showSnackbar(it)
                    }
                }

                if (currentUser == null) {
                    AuthScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = when (currentRole) {
                                                UserRole.CONSIGNEE -> "Shipper Portal"
                                                UserRole.VEHICLE_OWNER -> "Driver Portal"
                                                UserRole.ADMIN -> "Admin Command"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Text(
                                            text = "${currentUser?.name}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                actions = {
                                    // Synchronize Debug Data Button
                                    IconButton(
                                        onClick = { viewModel.synchronizeDebugData() },
                                        modifier = Modifier.testTag("appbar_sync_debug")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Sync,
                                            contentDescription = "Sync Debug",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Dynamic inline Quick-Switcher for the Portals & User Account Menu
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        var showUserMenu by remember { mutableStateOf(false) }
                                        AssistChip(
                                            onClick = { showUserMenu = true },
                                            label = {
                                                Text(
                                                    text = when (currentRole) {
                                                        UserRole.ADMIN -> "Admin"
                                                        UserRole.VEHICLE_OWNER -> "Driver"
                                                        UserRole.CONSIGNEE -> "Shipper"
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
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.ArrowDropDown,
                                                    contentDescription = "Menu",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            modifier = Modifier.testTag("appbar_role_chip")
                                        )

                                        DropdownMenu(
                                            expanded = showUserMenu,
                                            onDismissRequest = { showUserMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Switch to Shipper Portal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                                onClick = {
                                                    viewModel.quickDemoLogin(UserRole.CONSIGNEE)
                                                    showUserMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Switch to Driver Portal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                leadingIcon = { Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                                onClick = {
                                                    viewModel.quickDemoLogin(UserRole.VEHICLE_OWNER)
                                                    showUserMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Switch to Admin Portal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                leadingIcon = { Icon(Icons.Filled.SupervisorAccount, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                                onClick = {
                                                    viewModel.quickDemoLogin(UserRole.ADMIN)
                                                    showUserMenu = false
                                                }
                                            )
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Synchronize Debug Data", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                leadingIcon = { Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                                onClick = {
                                                    viewModel.synchronizeDebugData()
                                                    showUserMenu = false
                                                }
                                            )
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Sign Out", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                                                leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                                onClick = {
                                                    viewModel.signOut()
                                                    showUserMenu = false
                                                }
                                            )
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
}
