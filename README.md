# FieldCapture — Offline-First Material Delivery Mobile Prototype

> **Production-grade offline-first mobile prototype for construction material delivery field-capture, built with real SQLite local persistence, automatic sync, restart recovery, and PostgreSQL deduplication.**

---

## 1. Project Overview

**FieldCapture** is an offline-first mobile application designed specifically for construction job sites with intermittent or zero cellular connectivity. Construction foremen frequently receive vital material deliveries (rebar, structural steel, ready-mix concrete, masonry, lumber) in deep excavations, concrete basements, or remote zones where network connectivity is unavailable.

FieldCapture enables foremen to immediately photograph physical delivery tickets, enter supplier and purchase order data, log delivery notes, and store them locally on the device with 100% guarantee of data preservation. When network connectivity is restored, the dedicated **Sync Manager** automatically transmits queued payloads and photos to the backend PostgreSQL database using idempotency keys to strictly prevent duplicate entries.

---

## 2. Key Features

- **Genuine Offline-First Persistence**: Powered by a local SQLite database (Room). Deliveries are always written to the local database before any network activity is attempted.
- **Permanent Local Photo Storage**: Photos of delivery tickets are copied directly to application-sandboxed persistent storage and stored as local file URIs, guaranteeing they render on-device even in airplane mode.
- **Dedicated Sync Manager**: Automatically detects network availability (Android `ConnectivityManager` callbacks) and transitions queued records to `UPLOADING` and `SYNCED` upon connection return.
- **Interrupted Upload & App-Restart Recovery**: If the application is terminated while a delivery is actively uploading (`UPLOADING` status), the database initialization automatically recovers the record back to `QUEUED` with an audit error explanation, preventing lost deliveries.
- **Duplicate-Submission & Idempotency Protection**:
  - UI-level submission throttling prevents double-taps.
  - Every delivery record generates a cryptographically unique `idempotencyKey` (UUIDv4).
  - The PostgreSQL database enforces `UNIQUE(idempotency_key)`. When an already-processed delivery is re-transmitted (e.g. following network timeouts or retries), the server returns the existing record rather than creating a duplicate.
- **Offline Queue & Detailed Status**: Clear statuses (`QUEUED`, `UPLOADING`, `SYNCED`, `FAILED`), filterable tabs, error diagnostics, and one-tap retry for individual records or entire queues.
- **Interactive Developer / Demo Testing Suite**: Built directly into the mobile interface to allow rapid assessment of:
  - Forced Offline Mode
  - Forced Online Mode
  - Simulated HTTP 500 Outage
  - App-Restart Crash Recovery

---

## 3. Architecture

```text
┌────────────────────────────────────────────────────────┐
│                   Mobile Client                        │
│                                                        │
│  [Record Delivery UI]                                  │
│         │                                              │
│         ▼                                              │
│  [Local SQLite Database (Room)]  ◄─── Source of Truth  │
│         │                                              │
│         ▼                                              │
│  [Offline Sync Manager] ◄──── [ConnectivityManager]   │
│         │                                              │
└─────────┼──────────────────────────────────────────────┘
          │ (Multipart POST with idempotencyKey + photo)
          ▼
┌────────────────────────────────────────────────────────┐
│                   Backend Service                      │
│                                                        │
│  [Node.js + Express + TypeScript API]                  │
│         │                                              │
│         ├── Photo Storage: /uploads/ticket-*.jpg       │
│         │                                              │
│         ▼                                              │
│  [PostgreSQL Database]                                 │
│  deliveries table (UNIQUE idempotency_key)             │
└────────────────────────────────────────────────────────┘
```

### Why SQLite is the Local Source of Truth
In an offline-first architecture, treating the remote API as the primary data store leads to brittle user experiences, lost input, and UI freezing. In FieldCapture:
1. **Local write guarantee**: User actions complete immediately and durably in local SQLite within milliseconds regardless of radio conditions.
2. **Decoupled synchronization**: The Sync Manager operates as an independent background worker that reacts to database changes and network lifecycle events.
3. **Auditability**: Local sync attempts, timestamps, and error messages are recorded per delivery, giving site personnel clear feedback on delivery synchronization state.

---

## 4. Repository Structure

```text
├── app/                                 # Mobile Application (Android / Kotlin / Jetpack Compose)
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt              # App entrypoint & navigation state
│   │   ├── data/
│   │   │   ├── model/Delivery.kt        # Room Entity, DeliveryStatus, DeliveryCounts
│   │   │   ├── database/                # FieldCaptureDatabase, DeliveryDao, Converters
│   │   │   ├── repository/              # DeliveryRepository
│   │   │   └── storage/                 # PhotoStorageService (Local image persistence & ticket generator)
│   │   ├── sync/
│   │   │   ├── NetworkMonitor.kt        # Network capabilities listener & test simulation modes
│   │   │   ├── SyncManager.kt           # Background sync queue, retry, and crash recovery
│   │   │   └── ApiClient.kt             # HTTP client with idempotency handling
│   │   └── ui/
│   │       ├── MainViewModel.kt         # Reactive state management
│   │       ├── components/              # NetworkStatusBar, DeliveryStatusBadge, DeliveryItemCard
│   │       ├── screens/                 # HomeScreen, RecordDeliveryScreen, QueueScreen, DeliveryDetailScreen
│   │       └── theme/                   # High-contrast industrial design palette
│   └── build.gradle.kts
│
├── server/                              # Backend REST API (Node.js + Express + TypeScript)
│   ├── src/
│   │   ├── index.ts                     # Express server entrypoint & health checks
│   │   ├── db.ts                        # PostgreSQL connection pool & schema bootstrapper
│   │   └── routes/deliveries.ts         # Idempotent POST /api/deliveries & GET routes
│   ├── schema.sql                       # DDL for PostgreSQL table & indexes
│   ├── package.json
│   ├── tsconfig.json
│   └── .env.example
│
├── .env.example                         # Combined environment variable references
└── README.md
```

---

## 5. Setup & Running

### Mobile Application
The mobile application is ready to run directly in the AI Studio cloud streaming emulator or locally in Android Studio.

To build and compile:
```bash
gradle assembleDebug
```

### Backend Server (Node.js + PostgreSQL)
1. Navigate to the server folder:
   ```bash
   cd server
   npm install
   ```

2. Configure environment:
   ```bash
   cp .env.example .env
   # Edit DATABASE_URL and PORT if needed
   ```

3. Ensure PostgreSQL is running and initialize schema:
   ```bash
   psql -U postgres -d fieldcapture -f schema.sql
   ```

4. Start development server:
   ```bash
   npm run dev
   ```

The server exposes:
- Health check: `GET http://localhost:3000/api/health`
- Create delivery: `POST http://localhost:3000/api/deliveries`
- List deliveries: `GET http://localhost:3000/api/deliveries`
- Fetch by ID: `GET http://localhost:3000/api/deliveries/:id`

---

## 6. Testing & Demonstration Guide

### 1. Offline Material Capture
1. In the app, open the **Demo & Assessment Test Controls** (tap the tuning icon in the top right).
2. Select **Force Offline**. Observe the top status bar change to **Offline** with an amber indicator.
3. Tap **Record Material Delivery**. Notice the top banner states: *"You're offline — Your delivery will be saved on this device and synced automatically when you're back online."*
4. Tap **Capture Ticket** (or choose from device gallery). Enter Supplier (e.g. `Vulcan Materials`), PO Number (e.g. `PO-4481-B`), and delivery notes.
5. Tap **Save Delivery**. The app immediately transitions to the **Offline Queue**.
6. Verify status displays **QUEUED** (*"Waiting for connection"*).

### 2. Automatic Reconnection & Synchronization
1. Return to the Home screen or keep the Queue screen open.
2. Under Test Controls, toggle network mode to **Force Online** (or **Auto**).
3. The Network Monitor automatically detects connectivity. The Sync Manager instantly transitions the delivery status from **QUEUED** → **UPLOADING** (with progress indicator) → **SYNCED** (green checkmark).
4. Tap on the delivery to open **Delivery Details**. Notice the assigned **PostgreSQL Server ID**, the **Idempotency Key**, and confirmation that the item is permanently synchronized.

### 3. App Killed During Upload Recovery (Section 12)
1. Under Test Controls, tap **Test Crash Recovery**.
2. This simulates an application process being killed while an upload is in the `UPLOADING` state.
3. The database recovery query executes: any delivery stuck in `UPLOADING` is safely reverted to `QUEUED` with the message: *"Upload interrupted by app restart — queued for retry"*.
4. Once online, the Sync Manager picks it up and retries the upload without data loss.

### 4. Duplicate Submission Protection (Section 13)
1. The **Save Delivery** button disables during local write operations to prevent double-clicks.
2. Every delivery has a client-generated UUIDv4 `idempotencyKey`.
3. If an upload is retried or resent, the backend inspects `idempotency_key` against PostgreSQL.
4. When a match is found, the backend returns the existing delivery with `isDuplicate: true` and does not insert an additional row.

---

## 7. Deliverable Verification Checklist

- [x] Functional mobile app with Kotlin, Jetpack Compose, and Room (SQLite)
- [x] Local photo capture with persistent file storage and offline rendering
- [x] Dedicated Sync Manager with automatic retry on network reconnection
- [x] Edge-case handling for app killed mid-upload with state recovery
- [x] Client and server idempotency duplicate protection
- [x] Clean Node.js + Express + TypeScript backend with PostgreSQL schema
- [x] High-contrast enterprise Material 3 design optimized for field visibility
