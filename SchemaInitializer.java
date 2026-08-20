package common.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Runs once on startup — creates all 26 tables with
 * proper FK constraints, indexes, and normalization.
 */
public class SchemaInitializer {

    public static void initialize() {
        Connection conn = DBConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {

            // ── COMMON TABLES ─────────────────────────────────────────────────

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    full_name     TEXT    NOT NULL,
                    email         TEXT    NOT NULL UNIQUE,
                    phone         TEXT    NOT NULL,
                    created_at    TEXT    DEFAULT (datetime('now')),
                    is_active     INTEGER DEFAULT 1
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS admins (
                    admin_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    full_name     TEXT    NOT NULL,
                    role          TEXT    DEFAULT 'ADMIN',
                    created_at    TEXT    DEFAULT (datetime('now'))
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS login_history (
                    login_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER,
                    admin_id    INTEGER,
                    login_time  TEXT    DEFAULT (datetime('now')),
                    ip_address  TEXT,
                    user_type   TEXT    CHECK(user_type IN ('USER','ADMIN')),
                    FOREIGN KEY (user_id)  REFERENCES users(user_id),
                    FOREIGN KEY (admin_id) REFERENCES admins(admin_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_history (
                    payment_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_ref    TEXT    NOT NULL,
                    system_type    TEXT    CHECK(system_type IN ('RAILWAY','FLIGHT','BUS')),
                    amount         REAL    NOT NULL,
                    payment_method TEXT    CHECK(payment_method IN ('CASH','UPI')),
                    payment_status TEXT    CHECK(payment_status IN ('SUCCESS','FAILED','REFUNDED')),
                    transaction_id TEXT    UNIQUE,
                    paid_at        TEXT    DEFAULT (datetime('now'))
                )""");

            // ── RAILWAY TABLES ────────────────────────────────────────────────

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS railway_routes (
                    route_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    origin      TEXT NOT NULL,
                    destination TEXT NOT NULL,
                    distance_km REAL NOT NULL,
                    UNIQUE(origin, destination)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS railway_trains (
                    train_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    train_number TEXT    NOT NULL UNIQUE,
                    train_name   TEXT    NOT NULL,
                    route_id     INTEGER NOT NULL,
                    departure    TEXT    NOT NULL,
                    arrival      TEXT    NOT NULL,
                    total_seats  INTEGER NOT NULL,
                    status       TEXT    DEFAULT 'ACTIVE',
                    FOREIGN KEY (route_id) REFERENCES railway_routes(route_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS railway_coaches (
                    coach_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    train_id    INTEGER NOT NULL,
                    coach_name  TEXT    NOT NULL,
                    coach_type  TEXT    CHECK(coach_type IN ('SL','3A','2A','1A','CC','GEN')),
                    total_seats INTEGER NOT NULL,
                    fare_per_km REAL    NOT NULL,
                    FOREIGN KEY (train_id) REFERENCES railway_trains(train_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS railway_bookings (
                    booking_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    pnr          TEXT    NOT NULL UNIQUE,
                    user_id      INTEGER NOT NULL,
                    train_id     INTEGER NOT NULL,
                    coach_id     INTEGER NOT NULL,
                    journey_date TEXT    NOT NULL,
                    booking_date TEXT    DEFAULT (datetime('now')),
                    status       TEXT    CHECK(status IN ('CONFIRMED','WAITING','RAC','CANCELLED')),
                    total_fare   REAL    NOT NULL,
                    FOREIGN KEY (user_id)  REFERENCES users(user_id),
                    FOREIGN KEY (train_id) REFERENCES railway_trains(train_id),
                    FOREIGN KEY (coach_id) REFERENCES railway_coaches(coach_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS railway_passengers (
                    passenger_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id   INTEGER NOT NULL,
                    name         TEXT    NOT NULL,
                    age          INTEGER NOT NULL,
                    gender       TEXT    CHECK(gender IN ('M','F','O')),
                    seat_number  TEXT,
                    berth_type   TEXT    CHECK(berth_type IN ('LB','MB','UB','SL','SU','WIN','RAC')),
                    id_type      TEXT,
                    id_number    TEXT,
                    FOREIGN KEY (booking_id) REFERENCES railway_bookings(booking_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS railway_payments (
                    payment_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id     INTEGER NOT NULL UNIQUE,
                    amount         REAL    NOT NULL,
                    payment_method TEXT    CHECK(payment_method IN ('CASH','UPI')),
                    status         TEXT    CHECK(status IN ('SUCCESS','FAILED','REFUNDED')),
                    transaction_id TEXT    UNIQUE,
                    paid_at        TEXT    DEFAULT (datetime('now')),
                    FOREIGN KEY (booking_id) REFERENCES railway_bookings(booking_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS railway_waiting_list (
                    wl_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id   INTEGER NOT NULL,
                    train_id     INTEGER NOT NULL,
                    coach_id     INTEGER NOT NULL,
                    journey_date TEXT    NOT NULL,
                    wl_position  INTEGER NOT NULL,
                    created_at   TEXT    DEFAULT (datetime('now')),
                    FOREIGN KEY (booking_id) REFERENCES railway_bookings(booking_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS railway_rac (
                    rac_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id    INTEGER NOT NULL,
                    train_id      INTEGER NOT NULL,
                    coach_id      INTEGER NOT NULL,
                    journey_date  TEXT    NOT NULL,
                    rac_position  INTEGER NOT NULL,
                    seat_number   TEXT,
                    created_at    TEXT    DEFAULT (datetime('now')),
                    FOREIGN KEY (booking_id) REFERENCES railway_bookings(booking_id)
                )""");

            // ── FLIGHT TABLES ─────────────────────────────────────────────────

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS flight_routes (
                    route_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    origin      TEXT NOT NULL,
                    destination TEXT NOT NULL,
                    distance_km REAL NOT NULL,
                    UNIQUE(origin, destination)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS flight_details (
                    flight_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    flight_number TEXT    NOT NULL UNIQUE,
                    airline       TEXT    NOT NULL,
                    route_id      INTEGER NOT NULL,
                    departure     TEXT    NOT NULL,
                    arrival       TEXT    NOT NULL,
                    total_seats   INTEGER NOT NULL,
                    status        TEXT    DEFAULT 'SCHEDULED',
                    FOREIGN KEY (route_id) REFERENCES flight_routes(route_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS flight_seats (
                    seat_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    flight_id   INTEGER NOT NULL,
                    seat_number TEXT    NOT NULL,
                    class_type  TEXT    CHECK(class_type IN ('ECONOMY','BUSINESS','FIRST')),
                    is_booked   INTEGER DEFAULT 0,
                    FOREIGN KEY (flight_id) REFERENCES flight_details(flight_id),
                    UNIQUE(flight_id, seat_number)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS flight_bookings (
                    booking_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_ref  TEXT    NOT NULL UNIQUE,
                    user_id      INTEGER NOT NULL,
                    flight_id    INTEGER NOT NULL,
                    journey_date TEXT    NOT NULL,
                    class_type   TEXT    CHECK(class_type IN ('ECONOMY','BUSINESS','FIRST')),
                    booking_date TEXT    DEFAULT (datetime('now')),
                    status       TEXT    CHECK(status IN ('CONFIRMED','CANCELLED','PENDING')),
                    total_fare   REAL    NOT NULL,
                    FOREIGN KEY (user_id)   REFERENCES users(user_id),
                    FOREIGN KEY (flight_id) REFERENCES flight_details(flight_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS flight_passengers (
                    passenger_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id   INTEGER NOT NULL,
                    name         TEXT    NOT NULL,
                    age          INTEGER NOT NULL,
                    gender       TEXT    CHECK(gender IN ('M','F','O')),
                    seat_number  TEXT,
                    passport_no  TEXT,
                    nationality  TEXT,
                    FOREIGN KEY (booking_id) REFERENCES flight_bookings(booking_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS flight_payments (
                    payment_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id     INTEGER NOT NULL UNIQUE,
                    amount         REAL    NOT NULL,
                    payment_method TEXT    CHECK(payment_method IN ('CASH','UPI')),
                    status         TEXT    CHECK(status IN ('SUCCESS','FAILED','REFUNDED')),
                    transaction_id TEXT    UNIQUE,
                    paid_at        TEXT    DEFAULT (datetime('now')),
                    FOREIGN KEY (booking_id) REFERENCES flight_bookings(booking_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS flight_luggage (
                    luggage_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    passenger_id INTEGER NOT NULL,
                    booking_id   INTEGER NOT NULL,
                    weight_kg    REAL    NOT NULL,
                    extra_charge REAL    DEFAULT 0,
                    FOREIGN KEY (passenger_id) REFERENCES flight_passengers(passenger_id),
                    FOREIGN KEY (booking_id)   REFERENCES flight_bookings(booking_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS flight_meals (
                    meal_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    passenger_id INTEGER NOT NULL,
                    booking_id   INTEGER NOT NULL,
                    meal_type    TEXT    CHECK(meal_type IN ('VEG','NON_VEG','VEGAN','JAIN','DIABETIC')),
                    meal_charge  REAL    DEFAULT 0,
                    FOREIGN KEY (passenger_id) REFERENCES flight_passengers(passenger_id),
                    FOREIGN KEY (booking_id)   REFERENCES flight_bookings(booking_id)
                )""");

            // ── BUS TABLES ────────────────────────────────────────────────────

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bus_routes (
                    route_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    origin      TEXT NOT NULL,
                    destination TEXT NOT NULL,
                    distance_km REAL NOT NULL,
                    UNIQUE(origin, destination)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bus_details (
                    bus_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    bus_number   TEXT    NOT NULL UNIQUE,
                    bus_name     TEXT    NOT NULL,
                    route_id     INTEGER NOT NULL,
                    bus_type     TEXT    CHECK(bus_type IN ('SEATER','SLEEPER','AC_SEATER','AC_SLEEPER')),
                    departure    TEXT    NOT NULL,
                    arrival      TEXT    NOT NULL,
                    total_seats  INTEGER NOT NULL,
                    fare_per_km  REAL    NOT NULL,
                    status       TEXT    DEFAULT 'ACTIVE',
                    FOREIGN KEY (route_id) REFERENCES bus_routes(route_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bus_seats (
                    seat_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    bus_id      INTEGER NOT NULL,
                    seat_number TEXT    NOT NULL,
                    seat_type   TEXT    CHECK(seat_type IN ('WINDOW','AISLE','LOWER','UPPER')),
                    is_booked   INTEGER DEFAULT 0,
                    FOREIGN KEY (bus_id) REFERENCES bus_details(bus_id),
                    UNIQUE(bus_id, seat_number)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bus_bookings (
                    booking_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_ref  TEXT    NOT NULL UNIQUE,
                    user_id      INTEGER NOT NULL,
                    bus_id       INTEGER NOT NULL,
                    journey_date TEXT    NOT NULL,
                    booking_date TEXT    DEFAULT (datetime('now')),
                    status       TEXT    CHECK(status IN ('CONFIRMED','CANCELLED','PENDING')),
                    total_fare   REAL    NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(user_id),
                    FOREIGN KEY (bus_id)  REFERENCES bus_details(bus_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bus_passengers (
                    passenger_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id   INTEGER NOT NULL,
                    name         TEXT    NOT NULL,
                    age          INTEGER NOT NULL,
                    gender       TEXT    CHECK(gender IN ('M','F','O')),
                    seat_number  TEXT,
                    id_type      TEXT,
                    id_number    TEXT,
                    FOREIGN KEY (booking_id) REFERENCES bus_bookings(booking_id)
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bus_payments (
                    payment_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id     INTEGER NOT NULL UNIQUE,
                    amount         REAL    NOT NULL,
                    payment_method TEXT    CHECK(payment_method IN ('CASH','UPI')),
                    status         TEXT    CHECK(status IN ('SUCCESS','FAILED','REFUNDED')),
                    transaction_id TEXT    UNIQUE,
                    paid_at        TEXT    DEFAULT (datetime('now')),
                    FOREIGN KEY (booking_id) REFERENCES bus_bookings(booking_id)
                )""");

            // ── INDEXES ───────────────────────────────────────────────────────
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_railway_bookings_pnr      ON railway_bookings(pnr)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_railway_bookings_user     ON railway_bookings(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_railway_bookings_date     ON railway_bookings(journey_date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_railway_passengers_booking ON railway_passengers(booking_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_railway_wl_train_date     ON railway_waiting_list(train_id, journey_date)");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_flight_bookings_ref       ON flight_bookings(booking_ref)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_flight_bookings_user      ON flight_bookings(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_flight_seats_flight       ON flight_seats(flight_id)");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bus_bookings_ref          ON bus_bookings(booking_ref)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bus_bookings_user         ON bus_bookings(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bus_seats_bus             ON bus_seats(bus_id)");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_payment_history_ref       ON payment_history(booking_ref)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_login_history_user        ON login_history(user_id)");

            System.out.println("[DB] Schema initialized — all tables and indexes ready.");

        } catch (SQLException e) {
            throw new RuntimeException("[DB] Schema initialization failed: " + e.getMessage());
        }
    }
}