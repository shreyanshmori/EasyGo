package common.payment;

import common.database.DBConnection;
import common.exceptions.DatabaseException;
import datastructures.linkedlist.CustomLinkedList;

import java.sql.*;

/**
 * Data Access Object for the shared payment_history table.
 * All 3 systems (Railway, Flight, Bus) use this DAO.
 *
 * Operations: insert, updateStatus, findByRef, findByUser,
 *             findBySystem, refund, getAll.
 */
public class PaymentDAO {

    private final Connection conn;

    public PaymentDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── Insert new payment record ─────────────────────────────────────────────
    public int insert(String bookingRef, String systemType,
                      double amount, String method,
                      String status, String txnId) throws DatabaseException {
        String sql = """
            INSERT INTO payment_history
              (booking_ref, system_type, amount, payment_method, payment_status, transaction_id, paid_at)
            VALUES (?, ?, ?, ?, ?, ?, datetime('now'))
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bookingRef);
            ps.setString(2, systemType);
            ps.setDouble(3, amount);
            ps.setString(4, method);
            ps.setString(5, status);
            ps.setString(6, txnId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert payment record: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Update payment status (e.g. SUCCESS → REFUNDED) ─────────────────────
    public boolean updateStatus(String txnId, String newStatus) throws DatabaseException {
        String sql = "UPDATE payment_history SET payment_status = ? WHERE transaction_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, txnId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update payment status: " + e.getMessage(), e);
        }
    }

    // ── Find by booking reference ─────────────────────────────────────────────
    public PaymentModel findByRef(String bookingRef) throws DatabaseException {
        String sql = "SELECT * FROM payment_history WHERE booking_ref = ? ORDER BY paid_at DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingRef);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payment by ref: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Find by transaction ID ────────────────────────────────────────────────
    public PaymentModel findByTxnId(String txnId) throws DatabaseException {
        String sql = "SELECT * FROM payment_history WHERE transaction_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txnId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payment by txnId: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Find all payments for a system type ──────────────────────────────────
    public CustomLinkedList<PaymentModel> findBySystem(String systemType) throws DatabaseException {
        String sql = "SELECT * FROM payment_history WHERE system_type = ? ORDER BY paid_at DESC";
        CustomLinkedList<PaymentModel> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, systemType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch payments by system: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Find all payments (admin view) ────────────────────────────────────────
    public CustomLinkedList<PaymentModel> findAll() throws DatabaseException {
        String sql = "SELECT * FROM payment_history ORDER BY paid_at DESC";
        CustomLinkedList<PaymentModel> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all payments: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Total revenue by system ───────────────────────────────────────────────
    public double totalRevenue(String systemType) throws DatabaseException {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM payment_history "
                   + "WHERE system_type = ? AND payment_status = 'SUCCESS'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, systemType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to compute revenue: " + e.getMessage(), e);
        }
        return 0.0;
    }

    // ── Total refunds by system ───────────────────────────────────────────────
    public double totalRefunds(String systemType) throws DatabaseException {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM payment_history "
                   + "WHERE system_type = ? AND payment_status = 'REFUNDED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, systemType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to compute refunds: " + e.getMessage(), e);
        }
        return 0.0;
    }

    // ── Map ResultSet row → PaymentModel ─────────────────────────────────────
    private PaymentModel mapRow(ResultSet rs) throws SQLException {
        return new PaymentModel(
            rs.getInt("payment_id"),
            rs.getString("booking_ref"),
            rs.getString("system_type"),
            rs.getDouble("amount"),
            rs.getString("payment_method"),
            rs.getString("payment_status"),
            rs.getString("transaction_id"),
            rs.getString("paid_at")
        );
    }
}