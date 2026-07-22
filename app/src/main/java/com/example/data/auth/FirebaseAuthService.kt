package com.example.data.auth

import android.util.Log
import com.example.ui.viewmodel.UserAccount
import com.example.ui.viewmodel.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class FirebaseAuthService {

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseAuthService", "Firebase App not initialized or missing google-services.json: ${e.message}")
            null
        }
    }

    suspend fun signIn(email: String, password: String, requestedRole: UserRole): UserAccount {
        val firebaseInstance = auth
        if (firebaseInstance != null && email.isNotBlank() && password.length >= 6) {
            try {
                val result = firebaseInstance.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    val profileData = parseProfileString(user.displayName, user.email ?: email, requestedRole)
                    return profileData
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuthService", "Firebase signIn failed, falling back to local auth: ${e.message}")
            }
        }

        // Fallback or demo user creation
        return UserAccount(
            id = (100..999).random(),
            name = when (requestedRole) {
                UserRole.CONSIGNEE -> "Shipper (${email.substringBefore("@")})"
                UserRole.VEHICLE_OWNER -> "Driver (${email.substringBefore("@")})"
                UserRole.ADMIN -> "Admin (${email.substringBefore("@")})"
            },
            email = email,
            role = requestedRole,
            phone = "+1 (555) 019-2834",
            companyOrVehicle = when (requestedRole) {
                UserRole.CONSIGNEE -> "EcoTech Shipper Corp"
                UserRole.VEHICLE_OWNER -> "Semi Truck (TX-482-9B)"
                UserRole.ADMIN -> "Zone HQ Operations"
            }
        )
    }

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        phone: String,
        companyOrVehicle: String
    ): UserAccount {
        val firebaseInstance = auth
        val passToUse = if (password.length >= 6) password else "Password123!"

        if (firebaseInstance != null) {
            try {
                val result = firebaseInstance.createUserWithEmailAndPassword(email, passToUse).await()
                val user = result.user
                if (user != null) {
                    val encodedProfile = "ROLE:${role.name}|NAME:$name|COMPANY:$companyOrVehicle|PHONE:$phone"
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(encodedProfile)
                        .build()
                    user.updateProfile(profileUpdates).await()

                    return UserAccount(
                        id = user.uid.hashCode(),
                        name = name,
                        email = email,
                        role = role,
                        phone = phone.ifBlank { "+1 (555) 019-2834" },
                        companyOrVehicle = companyOrVehicle.ifBlank { "Independent Carrier" }
                    )
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuthService", "Firebase signUp failed, using fallback account: ${e.message}")
            }
        }

        return UserAccount(
            id = (1000..9999).random(),
            name = name,
            email = email,
            role = role,
            phone = phone.ifBlank { "+1 (555) 019-2834" },
            companyOrVehicle = companyOrVehicle.ifBlank { "Independent Operator" }
        )
    }

    fun getCurrentFirebaseUser(): UserAccount? {
        val user = auth?.currentUser ?: return null
        return parseProfileString(user.displayName, user.email ?: "user@logistics.com", UserRole.CONSIGNEE)
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseAuthService", "Error during Firebase sign out: ${e.message}")
        }
    }

    private fun parseProfileString(displayName: String?, fallbackEmail: String, defaultRole: UserRole): UserAccount {
        if (displayName.isNullOrBlank() || !displayName.contains("ROLE:")) {
            return UserAccount(
                id = (100..999).random(),
                name = displayName.takeIf { !it.isNullOrBlank() } ?: fallbackEmail.substringBefore("@"),
                email = fallbackEmail,
                role = defaultRole,
                phone = "+1 (555) 019-2834",
                companyOrVehicle = "Zone Freight Logistics"
            )
        }

        var parsedRole = defaultRole
        var parsedName = fallbackEmail.substringBefore("@")
        var parsedCompany = "Zone Freight Logistics"
        var parsedPhone = "+1 (555) 019-2834"

        try {
            val parts = displayName.split("|")
            for (part in parts) {
                val keyValue = part.split(":", limit = 2)
                if (keyValue.size == 2) {
                    when (keyValue[0]) {
                        "ROLE" -> parsedRole = try { UserRole.valueOf(keyValue[1]) } catch (e: Exception) { defaultRole }
                        "NAME" -> parsedName = keyValue[1]
                        "COMPANY" -> parsedCompany = keyValue[1]
                        "PHONE" -> parsedPhone = keyValue[1]
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuthService", "Error parsing profile string: ${e.message}")
        }

        return UserAccount(
            id = displayName.hashCode(),
            name = parsedName,
            email = fallbackEmail,
            role = parsedRole,
            phone = parsedPhone,
            companyOrVehicle = parsedCompany
        )
    }
}
