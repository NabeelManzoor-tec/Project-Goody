package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.LogisticsViewModel
import com.example.ui.viewmodel.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: LogisticsViewModel,
    modifier: Modifier = Modifier
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.CONSIGNEE) }

    // Form fields
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var companyOrVehicle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Logo & Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = "Logistics Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Zone Logistics Hub",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Multi-Portal Freight & Automated Logistics System",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Role Portal Selector Chips
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Portal Type",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == UserRole.CONSIGNEE,
                            onClick = { selectedRole = UserRole.CONSIGNEE },
                            label = { Text("Shipper", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("auth_role_shipper")
                        )
                        FilterChip(
                            selected = selectedRole == UserRole.VEHICLE_OWNER,
                            onClick = { selectedRole = UserRole.VEHICLE_OWNER },
                            label = { Text("Driver", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("auth_role_driver")
                        )
                        FilterChip(
                            selected = selectedRole == UserRole.ADMIN,
                            onClick = { selectedRole = UserRole.ADMIN },
                            label = { Text("Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.SupervisorAccount,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("auth_role_admin")
                        )
                    }
                }
            }

            // Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sign In / Sign Up Mode Switcher
                    TabRow(
                        selectedTabIndex = if (isSignUpMode) 1 else 0,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = !isSignUpMode,
                            onClick = { isSignUpMode = false; errorMessage = null },
                            text = {
                                Text(
                                    "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("tab_sign_in")
                                )
                            }
                        )
                        Tab(
                            selected = isSignUpMode,
                            onClick = { isSignUpMode = true; errorMessage = null },
                            text = {
                                Text(
                                    "Sign Up",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("tab_sign_up")
                                )
                            }
                        )
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Form Fields
                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name / Business Name") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_full_name")
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_email")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_password")
                    )

                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_phone")
                        )

                        OutlinedTextField(
                            value = companyOrVehicle,
                            onValueChange = { companyOrVehicle = it },
                            label = {
                                Text(
                                    when (selectedRole) {
                                        UserRole.CONSIGNEE -> "Company / Shipping Business"
                                        UserRole.VEHICLE_OWNER -> "Vehicle Model / Fleet Name"
                                        UserRole.ADMIN -> "Department / Org Name"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    when (selectedRole) {
                                        UserRole.CONSIGNEE -> Icons.Filled.Business
                                        UserRole.VEHICLE_OWNER -> Icons.Filled.LocalShipping
                                        UserRole.ADMIN -> Icons.Filled.SupervisorAccount
                                    },
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_company")
                        )
                    }

                    // Main Submit Action
                    Button(
                        onClick = {
                            if (!isSignUpMode) {
                                if (email.isBlank()) {
                                    errorMessage = "Please enter your email address"
                                } else {
                                    viewModel.signIn(email = email, password = password, role = selectedRole)
                                }
                            } else {
                                if (fullName.isBlank() || email.isBlank()) {
                                    errorMessage = "Please fill in all required fields"
                                } else {
                                    viewModel.signUp(
                                        name = fullName,
                                        email = email,
                                        password = password,
                                        role = selectedRole,
                                        phone = phone,
                                        companyOrVehicle = companyOrVehicle
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_auth_submit"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isSignUpMode) Icons.Filled.PersonAdd else Icons.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSignUpMode) {
                                "Register for ${when (selectedRole) {
                                    UserRole.CONSIGNEE -> "Shipper"
                                    UserRole.VEHICLE_OWNER -> "Driver"
                                    UserRole.ADMIN -> "Admin"
                                }} Portal"
                            } else {
                                "Sign In to ${when (selectedRole) {
                                    UserRole.CONSIGNEE -> "Shipper"
                                    UserRole.VEHICLE_OWNER -> "Driver"
                                    UserRole.ADMIN -> "Admin"
                                }} Portal"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Demo Login Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "⚡ Instant Demo Portal Access",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                    Text(
                        text = "Tap below to immediately test pre-configured user portals:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    )

                    OutlinedButton(
                        onClick = { viewModel.quickDemoLogin(UserRole.CONSIGNEE) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_quick_shipper"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shipper Portal (EcoTech Corp)", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.quickDemoLogin(UserRole.VEHICLE_OWNER) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_quick_driver"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Driver Portal (Dave Miller Fleet)", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.quickDemoLogin(UserRole.ADMIN) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_quick_admin"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.SupervisorAccount, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Admin Control Portal", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Synchronize Debug Data Button
            TextButton(
                onClick = { viewModel.synchronizeDebugData() },
                modifier = Modifier.testTag("btn_auth_sync_debug")
            ) {
                Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Synchronize & Reset Seed Debug Data", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
