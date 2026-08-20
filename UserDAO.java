package common.database;

import common.exceptions.DatabaseException;
import common.utilities.PasswordUtils;
import datastructures.linkedlist.CustomLinkedList;

import java.sql.*;

/**
 * UserDAO — CRUD for users and admins tables.
 * Used by AuthService for login / signup / profile management.
 */
public class UserDAO {

    private final Connection conn;

    public UserDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  USER OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    // ── Register new user ─────────────────────────────────────────────────────
    public int registerUser(String username, String password,
                            String fullName, String email, String phone)
            throws DatabaseException {
        String sql = """
            INSERT INTO users (username, password_hash, full_name, email, phone)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtils.hash(password));
            ps.setString(3, fullName);
            ps.setString(4, email.toLowerCase());
            ps.setString(5, phone);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE"))
                throw new DatabaseException("Username or email already exists.");
            throw new DatabaseException("Register user failed: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Login user ────────────────────────────────────────────────────────────
    public int[] loginUser(String username, String password) throws DatabaseException {
        String sql = "SELECT user_id, password_hash, is_active FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;                        // not found
                if (rs.getInt("is_active") == 0) return new int[]{-2}; // deactivated
                boolean match = PasswordUtils.verify(password, rs.getString("password_hash"));
                if (!match) return null;
                return new int[]{rs.getInt("user_id")};
            }
        } catch (SQLException e) {
            throw new DatabaseException("Login failed: " + e.getMessage(), e);
        }
    }

    // ── Get user profile ──────────────────────────────────────────────────────
    public String[] getUserProfile(int userId) throws DatabaseException {
        String sql = "SELECT username, full_name, email, phone, created_at FROM users WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new String[]{
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("created_at")
                };
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get profile failed: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Update password ───────────────────────────────────────────────────────
    public boolean updatePassword(int userId, String newPassword) throws DatabaseException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PasswordUtils.hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Update password failed: " + e.getMessage(), e);
        }
    }

    // ── Update profile ────────────────────────────────────────────────────────
    public boolean updateProfile(int userId, String fullName,
                                 String email, String phone) throws DatabaseException {
        String sql = "UPDATE users SET full_name=?, email=?, phone=? WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email.toLowerCase());
            ps.setString(3, phone);
            ps.setInt   (4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Update profile failed: " + e.getMessage(), e);
        }
    }

    // ── Deactivate user ───────────────────────────────────────────────────────
    public boolean deactivateUser(int userId) throws DatabaseException {
        String sql = "UPDATE users SET is_active = 0 WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Deactivate user failed: " + e.getMessage(), e);
        }
    }

    // ── Get all users (admin) ─────────────────────────────────────────────────
    public CustomLinkedList<String[]> getAllUsers() throws DatabaseException {
        String sql = "SELECT user_id, username, full_name, email, phone, created_at, is_active FROM users ORDER BY user_id";
        CustomLinkedList<String[]> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new String[]{
                    String.valueOf(rs.getInt("user_id")),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("created_at"),
                    rs.getInt("is_active") == 1 ? "ACTIVE" : "INACTIVE"
                });
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get all users failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Check username exists ─────────────────────────────────────────────────
    public boolean usernameExists(String username) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Check username: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ADMIN OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    // ── Register admin ────────────────────────────────────────────────────────
    public int registerAdmin(String username, String password,
                             String fullName, String role) throws DatabaseException {
        String sql = "INSERT INTO admins (username, password_hash, full_name, role) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtils.hash(password));
            ps.setString(3, fullName);
            ps.setString(4, role);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE"))
                throw new DatabaseException("Admin username already exists.");
            throw new DatabaseException("Register admin failed: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Login admin ───────────────────────────────────────────────────────────
    public int[] loginAdmin(String username, String password) throws DatabaseException {
        String sql = "SELECT admin_id, password_hash FROM admins WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                boolean match = PasswordUtils.verify(password, rs.getString("password_hash"));
                if (!match) return null;
                return new int[]{rs.getInt("admin_id")};
            }
        } catch (SQLException e) {
            throw new DatabaseException("Admin login failed: " + e.getMessage(), e);
        }
    }

    // ── Check if any admin exists (first-run seed) ────────────────────────────
    public boolean adminExists() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM admins";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Check admin: " + e.getMessage(), e);
        }
    }

    // ── Get admin profile ─────────────────────────────────────────────────────
    public String[] getAdminProfile(int adminId) throws DatabaseException {
        String sql = "SELECT username, full_name, role, created_at FROM admins WHERE admin_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new String[]{
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("created_at")
                };
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get admin profile: " + e.getMessage(), e);
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGIN HISTORY
    // ══════════════════════════════════════════════════════════════════════════

    public void recordLogin(int userId, boolean isAdmin) throws DatabaseException {
        String sql = """
            INSERT INTO login_history (user_id, admin_id, user_type)
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (isAdmin) {
                ps.setNull  (1, Types.INTEGER);
                ps.setInt   (2, userId);
                ps.setString(3, "ADMIN");
            } else {
                ps.setInt   (1, userId);
                ps.setNull  (2, Types.INTEGER);
                ps.setString(3, "USER");
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Record login failed: " + e.getMessage(), e);
        }
    }

    // ── Get login history (admin view) ────────────────────────────────────────
    public CustomLinkedList<String[]> getLoginHistory(int limit) throws DatabaseException {
        String sql = """
            SELECT lh.login_id, lh.user_type,
                   COALESCE(u.username, a.username) AS username,
                   lh.login_time
            FROM login_history lh
            LEFT JOIN users  u ON lh.user_id  = u.user_id
            LEFT JOIN admins a ON lh.admin_id = a.admin_id
            ORDER BY lh.login_time DESC
            LIMIT ?
            """;
        CustomLinkedList<String[]> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new String[]{
                    String.valueOf(rs.getInt("login_id")),
                    rs.getString("user_type"),
                    rs.getString("username"),
                    rs.getString("login_time")
                });
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get login history: " + e.getMessage(), e);
        }
        return list;
    }
}
