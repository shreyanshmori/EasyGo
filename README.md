[README.md](https://github.com/user-attachments/files/31281929/README.md)
## 🚆✈️🚌 EASYGO

A complete **console-based** Transport Reservation Management System built with **Java 17**, **SQLite**, **JDBC**, and **custom-implemented Data Structures** — no GUI, no Spring Boot, no web frameworks, no Java Collections Framework.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Systems Included](#systems-included)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Build & Run](#build--run)
- [First Run](#first-run)
- [Default Credentials](#default-credentials)
- [Application Flow](#application-flow)
- [Database Schema](#database-schema)
- [Custom Data Structures](#custom-data-structures)
- [Key Features](#key-features)
- [Payment Gateway](#payment-gateway)
- [Cancellation & Refund Policy](#cancellation--refund-policy)
- [Generated Files](#generated-files)
- [Architecture](#architecture)
- [Design Patterns](#design-patterns)
- [Sample Data](#sample-data)

---

## Overview

This project is an **industry-level, resume-ready**, console-based transport reservation system that integrates:

- **3 reservation systems** — Railway, Flight, Bus
- **1 shared payment gateway** — Cash + UPI with ASCII QR code
- **26 database tables** with foreign keys, indexes, and normalization
- **10 custom data structures** — implemented from scratch without Java Collections
- **Full JDBC transaction management** — commit, rollback, prepared statements
- **Modular architecture** — DAO + Service + Model layers

---

## Systems Included

### 🚆 Railway Reservation System
- Train search by origin/destination with **DoublyLinkedList** date navigation (prev/next)
- Seat layout display for SL, 3A, 2A, 1A, CC, GEN coaches
- CONFIRMED → RAC → Waiting List booking with auto-promotion on cancellation
- PNR status check with passenger details
- Ticket file generation (`.txt`)

### ✈️ Flight Reservation System
- Economy, Business, First Class booking
- **Dynamic pricing** — 5-factor fare engine (base × demand × time × season × GST)
- Demand surge using **CustomHeap** priority tiers
- Meal selection (VEG / NON_VEG / VEGAN / JAIN / DIABETIC)
- Luggage management with free allowance and extra charges
- ASCII boarding pass with barcode simulation and gate assignment

### 🚌 Bus Reservation System
- Sleeper (lower/upper berth) and Seater (window/aisle) layouts
- **Graph + Dijkstra** shortest path for distance-based fare calculation
- Seat preference selection (lower berth / window)
- Route network visualization
- Admin shortest path fare calculator

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 (text blocks, switch expressions, records) |
| Database | SQLite 3 |
| DB Driver | `sqlite-jdbc 3.45.1.0` |
| DB Access | JDBC — PreparedStatements, Transactions, Rollback |
| Build | Maven (`pom.xml`) **or** Gradle (`build.gradle`) |
| Data Structures | All custom — no `java.util` collections used |
| Password Hashing | Custom SHA-256 implementation (no libraries) |

---

## Prerequisites

| Tool | Version | Check Command |
|------|---------|---------------|
| Java JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Gradle | 8+ (optional) | `gradle -version` |

> Only **one** of Maven or Gradle is needed. No other tools required.

---

## Project Structure

```
EasyGo/
├── pom.xml                     ← Maven build
├── build.gradle                ← Gradle build (alternative)
├── .gitignore
├── README.md
├── easygo.db                   ← SQLite DB (auto-created on first run)
├── tickets/                    ← Generated ticket files (auto-created)
│
├── Main.java                   ← Application entry point
├── common/
│   ├── database/               → DBConnection, SchemaInitializer, UserDAO
│   ├── exceptions/             → Custom exception classes
│   ├── validation/             → Validator
│   ├── payment/                → PaymentGateway, QRGenerator, RefundPolicy, PaymentService
│   ├── utilities/              → ConsoleUtils, DateUtils, PasswordUtils, AuthManager, AuthService
│   └── filehandling/           → TicketFileWriter
├── datastructures/
│   ├── linkedlist/             → Custom linked lists
│   ├── queue/                  → Custom queues
│   ├── stack/                  → CustomStack
│   ├── hashmap/                → CustomHashMap
│   ├── heap/                   → CustomHeap
│   ├── graph/                  → Graph + Dijkstra
│   └── trees/                  → BinarySearchTree
├── railway/
│   ├── models/
│   ├── dao/
│   ├── services/
│   ├── management/
│   ├── tickets/
│   └── RailwayMain.java
├── flight/
│   ├── models/
│   ├── dao/
│   ├── services/
│   ├── tickets/
│   └── FlightMain.java
└── bus/
    ├── models/
    ├── dao/
    ├── services/
    ├── tickets/
    └── BusMain.java
```

---

## Build & Run

### Option A — Maven (Recommended)

```bash
# Compile
mvn compile

# Run
mvn exec:java

# OR build fat JAR and run
mvn package
java -jar target/easygo.jar
```

### Option B — Gradle

```bash
# Run directly
./gradlew run

# OR build fat JAR
./gradlew jar
java -jar build/libs/easygo.jar
```

### Option C — Manual `javac` (no build tool)

```bash
# Compile
bash run.sh
```

The manual script compiles all `.java` files from the project root and runs `Main`.

### IntelliJ IDEA Setup

1. **File → Open** → select `EasyGo/`
2. **Project Structure → SDK** → Java 17+
3. **Project Structure → Modules → Sources** → mark the project root as Sources Root
4. **Project Structure → Libraries** → Add the JAR files from `lib/` if you are not using Maven/Gradle
5. Right-click `Main.java` → **Run 'Main.main()'**

---

## First Run

On first launch the system automatically:

1. Creates `easygo.db` with all **26 tables** and **14 indexes**
2. Seeds a **default admin** account
3. Creates the `tickets/` directory for generated files

```
Expected console output:
  [DB] Connected to: easygo.db
  [DB] Schema initialized — all tables and indexes ready.
  [SYSTEM] Default admin created — username: admin | password: Admin@123
  [SYSTEM] System ready.
```

---

## Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `Admin@123` |

> ⚠️ Change the default password after first login via Admin Dashboard.

---

## Application Flow

```
Launch App
    │
    ▼
┌─────────────────────────────────────────────┐
│              MAIN MENU                       │
│  [1] User Login                             │
│  [2] Create Account (Signup)                │
│  [3] Admin Login                            │
│  [0] Exit                                   │
└─────────────────────────────────────────────┘
    │
    ├─── User Login ──────────────────────────────────────────────────────────┐
    │         ▼                                                               │
    │    ┌──────────────────────────────────┐                                 │
    │    │         USER MENU                │                                 │
    │    │  [1] Railway Reservation         │                                 │
    │    │  [2] Flight Reservation          │                                 │
    │    │  [3] Bus Reservation             │                                 │
    │    │  [4] My Profile                  │                                 │
    │    │  [5] Change Password             │                                 │
    │    │  [6] Logout                      │                                 │
    │    └──────────────────────────────────┘                                 │
    │                                                                         │
    └─── Admin Login ─────────────────────────────────────────────────────────┘
              ▼
         ┌──────────────────────────────────────────┐
         │           ADMIN DASHBOARD                 │
         │  [1] Railway Admin Panel                  │
         │  [2] Flight Admin Panel                   │
         │  [3] Bus Admin Panel                      │
         │  [4] Payment & Revenue Reports            │
         │  [5] User Management                      │
         │  [6] Login History                        │
         │  [7] System Statistics                    │
         │  [8] Seed All Sample Data                 │
         └──────────────────────────────────────────┘
```

---

## Database Schema

### Common Tables (4)
| Table | Purpose |
|-------|---------|
| `users` | Registered user accounts |
| `admins` | Admin accounts with roles |
| `payment_history` | All payments across systems |
| `login_history` | User/admin login audit trail |

### Railway Tables (8)
`railway_routes` · `railway_trains` · `railway_coaches` · `railway_bookings` · `railway_passengers` · `railway_payments` · `railway_waiting_list` · `railway_rac`

### Flight Tables (8)
`flight_routes` · `flight_details` · `flight_seats` · `flight_bookings` · `flight_passengers` · `flight_payments` · `flight_luggage` · `flight_meals`

### Bus Tables (6)
`bus_routes` · `bus_details` · `bus_seats` · `bus_bookings` · `bus_passengers` · `bus_payments`

**All tables include:**
- Foreign key constraints (`PRAGMA foreign_keys = ON`)
- Indexed columns for fast lookup
- CHECK constraints for valid enum values
- `datetime('now')` defaults for timestamps

---

## Custom Data Structures

All 10 data structures implemented from scratch — zero `java.util` collections used.

| # | Structure | File | Used For |
|---|-----------|------|----------|
| 1 | `CustomLinkedList<T>` | `linkedlist/` | All result lists (trains, bookings, passengers) |
| 2 | `DoublyLinkedList<T>` | `linkedlist/` | Railway **prev/next date navigation** |
| 3 | `CircularLinkedList<T>` | `linkedlist/` | Seat slot cycling |
| 4 | `CustomQueue<T>` | `queue/` | Railway **Waiting List** queue |
| 5 | `CircularQueue<T>` | `queue/` | **RAC buffer** (fixed capacity ring) |
| 6 | `CustomStack<T>` | `stack/` | Graph path reconstruction, booking undo |
| 7 | `CustomHashMap<K,V>` | `hashmap/` | Seat cache, fare cache, session store, layout grouping |
| 8 | `CustomHeap<T>` | `heap/` | Flight **dynamic pricing demand tiers** |
| 9 | `BinarySearchTree<T>` | `trees/` | O(log n) **train number lookup index** |
| 10 | `Graph` + Dijkstra | `graph/` | **Bus route network** + shortest path fare |

---

## Key Features

### Railway
- ✅ Train search by origin/destination
- ✅ Coach availability (SL, 3A, 2A, 1A, CC, GEN)
- ✅ Visual seat layout (berth map)
- ✅ CONFIRMED / RAC / Waiting List booking
- ✅ Auto-promotion on cancellation (WL→RAC→CONFIRMED)
- ✅ DoublyLinkedList date navigation (prev/next date)
- ✅ BST-indexed train number search
- ✅ PNR status check
- ✅ Ticket file generation

### Flight
- ✅ Flight search by route
- ✅ Economy / Business / First class
- ✅ 5-factor dynamic pricing engine
- ✅ Demand surge via CustomHeap tiers
- ✅ Meal selection (5 types)
- ✅ Luggage with free allowance + extra charges
- ✅ ASCII boarding pass with barcode
- ✅ Deterministic gate assignment

### Bus
- ✅ Bus search by route
- ✅ Sleeper and Seater layouts
- ✅ Graph + Dijkstra route network
- ✅ Distance-based fare with type multiplier
- ✅ Lower berth / window seat preference
- ✅ Bulk passenger discount
- ✅ Admin shortest path fare calculator

### Common
- ✅ User signup with full validation
- ✅ SHA-256 password hashing (custom implementation)
- ✅ JDBC transactions (commit / rollback)
- ✅ UPI QR code (ASCII 21×21 matrix)
- ✅ Payment receipt generation
- ✅ Login history audit trail
- ✅ Admin user management

---

## Payment Gateway

Single shared gateway used by all 3 systems.

### Cash Flow
1. Enter amount tendered by customer
2. System calculates change
3. Confirm receipt → booking confirmed

### UPI / QR Flow
1. ASCII QR code displayed with UPI deep-link
2. Customer scans with any UPI app (PhonePe, GPay, Paytm, BHIM)
3. Enter UTR / Transaction ID (12–22 alphanumeric chars)
4. System verifies → booking confirmed

### Safety Features
- Duplicate payment prevention
- 3-attempt rate limiter per booking reference
- UTR format validation
- Session-level payment cache (`CustomHashMap`)

---

## Cancellation & Refund Policy

### Railway
| Days Before Journey | Status | Refund |
|---------------------|--------|--------|
| 30+ days | CONFIRMED | 100% |
| 10–29 days | CONFIRMED | 75% |
| 4–9 days | CONFIRMED | 50% |
| 2–3 days | CONFIRMED | 25% |
| < 2 days | CONFIRMED | 0% |
| Any time | WAITING | 100% |
| > 2 days | RAC | 100% |
| ≤ 2 days | RAC | 50% |

### Flight
| Days Before | Economy | Business | First |
|-------------|---------|----------|-------|
| 14+ days | 75% | 85% | 90% |
| 7–13 days | 50% | 60% | 70% |
| 3–6 days | 20% | 30% | 40% |
| < 3 days | 0% | 0% | 0% |

### Bus
| Days Before | Non-AC | AC |
|-------------|--------|----|
| 7+ days | 90% | 85% |
| 3–6 days | 70% | 60% |
| 1–2 days | 40% | 30% |
| Same day | 0% | 0% |

---

## Generated Files

All output saved to `tickets/` directory (auto-created):

| File Pattern | Contents |
|-------------|---------|
| `tickets/RAIL_<PNR>.txt` | Railway e-ticket with passenger details |
| `tickets/FLT_<REF>.txt` | Flight boarding pass with barcode |
| `tickets/BUS_<REF>.txt` | Bus ticket with seat details |
| `tickets/RCPT_<REF>.txt` | Payment receipt with transaction ID |

---

## Architecture

```
┌────────────────────────────────────────────────────────┐
│                  Console UI Layer                       │
│    Main.java | RailwayMain | FlightMain | BusMain       │
└────────────────────────┬───────────────────────────────┘
                         │
┌────────────────────────▼───────────────────────────────┐
│                  Service Layer                          │
│  BookingService | SearchService | AdminService          │
│  DynamicPricingService | PaymentService                 │
└────────────────────────┬───────────────────────────────┘
                         │
┌────────────────────────▼───────────────────────────────┐
│                   DAO Layer                             │
│  TrainDAO | FlightDAO | BusDAO | BookingDAO             │
│  PaymentDAO | UserDAO | WaitingListDAO                  │
└────────────────────────┬───────────────────────────────┘
                         │
┌────────────────────────▼───────────────────────────────┐
│              SQLite Database Layer                      │
│                 easygo.db (26 tables)                  │
└────────────────────────────────────────────────────────┘
```

---

## Design Patterns

| Pattern | Where Used |
|---------|-----------|
| **Singleton** | `DBConnection` — one JDBC connection throughout the app |
| **DAO Pattern** | All DB logic isolated in `*DAO` classes |
| **Service Pattern** | Business logic in `*Service` classes, separated from UI and DB |
| **Facade** | `PaymentService` — unified pay/refund interface for all 3 systems |
| **DTO** | `TicketFileWriter.*TicketData` inner classes |
| **Strategy** | `RefundPolicy` — different refund rules per system |
| **Observer-like** | WL promotion chain — cancel triggers RAC→CONFIRMED→WL→RAC |

---

## Sample Data

After admin login, use **Seed All Sample Data** to load:

### Railway (5 trains)
| Number | Name | Route |
|--------|------|-------|
| 12301 | Rajdhani Express | Mumbai → Delhi |
| 12302 | Duronto Express | Delhi → Kolkata |
| 12027 | Shatabdi Express | Chennai → Bangalore |
| 19011 | Gujarat Mail | Mumbai → Ahmedabad |
| 12015 | Ajmer Shatabdi | Delhi → Jaipur |

### Flight (6 flights)
| Number | Airline | Route |
|--------|---------|-------|
| AI-101 | Air India | DEL → BOM |
| 6E-201 | IndiGo | BOM → DEL |
| AI-505 | Air India | DEL → BLR |
| SG-301 | SpiceJet | BOM → BLR |
| UK-201 | Vistara | DEL → HYD |
| 6E-888 | IndiGo | BOM → CCU |

### Bus (6 buses)
| Number | Name | Route | Type |
|--------|------|-------|------|
| MH-01 | Shivneri Express | Mumbai → Pune | AC Seater |
| DL-02 | Agra Volvo | Delhi → Agra | AC Seater |
| KA-03 | KSRTC Rajahamsa | Bangalore → Mysore | AC Seater |
| GJ-04 | Surat Sleeper | Ahmedabad → Surat | AC Sleeper |
| TN-05 | Coimbatore Night | Chennai → Coimbatore | Sleeper |
| RJ-06 | Udaipur Express | Jaipur → Udaipur | Seater |

---

## Common Issues

| Error | Fix |
|-------|-----|
| `UnsupportedClassVersionError` | Upgrade to Java 17+ |
| `ClassNotFoundException: org.sqlite.JDBC` | Use Maven/Gradle, or add the JAR files from `lib/` to the classpath |
| Scanner not reading input in IDE | Add `standardInput = System.in` in run config |
| `easygo.db` locked | Close any other DB viewers, restart app |
| `tickets/` permission error | Manually create `tickets/` folder in project root |

---

## License

MIT License — free to use for educational and portfolio purposes.

---

*Built with Java 17 · SQLite · JDBC · Custom DSA · No external frameworks*
=======
