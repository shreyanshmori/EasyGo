package common.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton SQLite Database Connection Manager.
 * Handles: connection lifecycle, foreign key enforcement,
 *          WAL mode, schema bootstrap.
 */
public class DBConnection {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String DB_FILE = "easygo.db";
    private static final String DB_URL  = "jdbc:sqlite:" + DB_FILE;

    // ── Singleton instance ────────────────────────────────────────────────────
    private static DBConnection instance = null;
    private Connection connection        = null;

    // ── Private constructor ───────────────────────────────────────────────────
    private DBConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            configure();
            System.out.println("[DB] Connected to: " + DB_FILE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("[DB] SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException("[DB] Connection failed: " + e.getMessage());
        }
    }

    // ── Get singleton instance ────────────────────────────────────────────────
    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) instance = new DBConnection();
            }
        }
        return instance;
    }

    // ── Get raw JDBC connection ───────────────────────────────────────────────
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                configure();
            }
        } catch (SQLException e) {
            throw new RuntimeException("[DB] Failed to reopen connection: " + e.getMessage());
        }
        return connection;
    }

    // ── Configure pragmas ─────────────────────────────────────────────────────
    private void configure() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = 10000");
            stmt.execute("PRAGMA temp_store = MEMORY");
        }
    }

    // ── Transaction helpers ───────────────────────────────────────────────────
    public void beginTransaction() throws SQLException {
        connection.setAutoCommit(false);
    }

    public void commit() throws SQLException {
        connection.commit();
        connection.setAutoCommit(true);
    }

    public void rollback() {
        try {
            if (connection != null && !connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Rollback failed: " + e.getMessage());
        }
    }

    // ── Close ─────────────────────────────────────────────────────────────────
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Close failed: " + e.getMessage());
        }
    }
}