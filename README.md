# Logistics Hub 🚚 Package & Freight Management Engine

**Logistics Hub** is a modern, native Android application built with **Jetpack Compose**, **Kotlin**, and **Room Database**. It provides an end-to-end logistics platform serving **Consignees/Shippers**, **Vehicle Owners/Drivers**, and **Logistics Administrators**.

---

## 🌟 Key Features

### 💬 Direct Driver-Consignee Communication Channel
* **Bidding Discussion Hub**: Drivers and consignees can chat immediately after a bid is placed to negotiate fares, cargo requirements, and schedules.
* **Instant In-App Chat & Quick Preset Chips**: Communicate pickup site status, ETA updates, and dock loading details with one-tap logistics preset chips.
* **Voice Notes & Audio Messaging**: Record and send voice messages directly within shipment chat threads.
* **In-App Voice Call Simulator & Cellular Dialer**: Simulated voice call interface with mute/speaker toggles and quick access to native cellular dialing.
* **Central Communication Center**: App bar action hub displaying unread message counters across all active shipment threads.

### 🚛 Automated Freight Matching & Bidding Engine
* **Instant Zone & Capacity Matching**: Automatically pairs available cargo with nearby vehicles based on operating zone and weight limits.
* **Competitive Driver Bidding**: Drivers can place custom price bids on open shipments.
* **One-Tap Bid Acceptance**: Consignees review driver profiles, ratings, vehicle details, and accept bids in one tap.

### 📍 Shipment Lifecycle & Live Tracking
* **Milestone Progress Tracking**: Track shipments through *Pending Bids*, *In Transit*, *Out for Delivery*, and *Delivered*.
* **Digital Proof of Delivery (PoD)**: OTP verification and recipient signature capture upon arrival.
* **Automated Status Notifications**: Real-time app alerts when cargo status updates.

### 👥 Role-Based Portals
1. **Consignee / Shipper**: Post freight, evaluate bids, communicate directly with assigned drivers, and track incoming deliveries.
2. **Vehicle Owner / Driver**: Manage fleet vehicles, discover matching loads, submit competitive bids, and chat directly with shippers.
3. **Admin Platform**: Complete system overview for monitoring active shipments, vehicle verification, and support desk tickets.

---

## 🛠️ Tech Stack & Architecture

* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **Language**: [Kotlin](https://kotlinlang.org/)
* **Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room) with KSP
* **Asynchronous Streams**: Kotlin Coroutines & `StateFlow`
* **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern

---

## 📱 Project Structure

```
com.example/
├── data/
│   ├── dao/             # Room DAOs for Logistics, Bids, Vehicles, Chat
│   ├── database/        # AppDatabase configuration
│   ├── model/           # Room entities (ShipmentEntity, BidEntity, ChatMessageEntity, etc.)
│   └── repository/      # LogisticsRepository data orchestrator
├── ui/
│   ├── components/      # ChatAndCallDialog, VoiceCallDialog, CommunicationCenterDialog
│   ├── screens/         # DashboardScreen, ActiveShipmentsScreen, AuthScreen, SupportScreen
│   ├── theme/           # Material 3 Color Schemes & Typography
│   └── viewmodel/       # LogisticsViewModel & UI state management
└── MainActivity.kt      # Main entry point & Navigation host
```

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Jellyfish | 2023.3.1 or newer
* **JDK**: Version 17
* **Android SDK**: API level 24 (Android 7.0) or higher

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/logistics-hub.git
   cd logistics-hub
   ```

2. **Open in Android Studio**:
   Open Android Studio and select **Open**, then navigate to the cloned project folder.

3. **Build the Project**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run the Application**:
   Select an Android Emulator or connected physical device and click **Run (Shift + F10)**.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more details.
