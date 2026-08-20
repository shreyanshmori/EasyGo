package railway.dao;

import common.database.DBConnection;
import common.exceptions.DatabaseException;
import datastructures.linkedlist.CustomLinkedList;
import java.sql.*;

public class WaitingListDAO {

    private final Connection conn;

    public WaitingListDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── Insert WL entry ───────────────────────────────────────────────────────
    public int insertWL(int bookingId, int trainId, int coachId,
                        String journeyDate, int position) throws DatabaseException {
        String sql = """
            INSERT INTO railway_waiting_list
              (booking_id, train_id, coach_id, journey_date, wl_position)
            VALUES (?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, bookingId);
            ps.setInt   (2, trainId);
            ps.setInt   (3, coachId);
            ps.setString(4, journeyDate);
            ps.setInt   (5, position);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Insert WL failed: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Insert RAC entry ──────────────────────────────────────────────────────
    public int insertRAC(int bookingId, int trainId, int coachId,
                         String journeyDate, int position, String seatNum) throws DatabaseException {
        String sql = """
            INSERT INTO railway_rac
              (booking_id, train_id, coach_id, journey_date, rac_position, seat_number)
            VALUES (?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, bookingId);
            ps.setInt   (2, trainId);
            ps.setInt   (3, coachId);
            ps.setString(4, journeyDate);
            ps.setInt   (5, position);
            ps.setString(6, seatNum);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Insert RAC failed: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Get current WL count ──────────────────────────────────────────────────
    public int getWLCount(int trainId, int coachId, String date) throws DatabaseException {
        String sql = """
            SELECT COUNT(*) FROM railway_waiting_list
            WHERE train_id=? AND coach_id=? AND journey_date=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId); ps.setInt(2, coachId); ps.setString(3, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get WL count failed: " + e.getMessage(), e);
        }
        return 0;
    }

    // ── Get current RAC count ─────────────────────────────────────────────────
    public int getRACCount(int trainId, int coachId, String date) throws DatabaseException {
        String sql = """
            SELECT COUNT(*) FROM railway_rac
            WHERE train_id=? AND coach_id=? AND journey_date=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId); ps.setInt(2, coachId); ps.setString(3, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get RAC count failed: " + e.getMessage(), e);
        }
        return 0;
    }

    // ── Get first WL booking (for promotion after cancellation) ───────────────
    public railway.models.WaitingListEntry getFirstWL(int trainId, int coachId, String date)
            throws DatabaseException {
        String sql = """
            SELECT * FROM railway_waiting_list
            WHERE train_id=? AND coach_id=? AND journey_date=?
            ORDER BY wl_position ASC LIMIT 1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId); ps.setInt(2, coachId); ps.setString(3, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapWL(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get first WL failed: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Get first RAC entry ───────────────────────────────────────────────────
    public railway.models.RACEntry getFirstRAC(int trainId, int coachId, String date)
            throws DatabaseException {
        String sql = """
            SELECT * FROM railway_rac
            WHERE train_id=? AND coach_id=? AND journey_date=?
            ORDER BY rac_position ASC LIMIT 1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId); ps.setInt(2, coachId); ps.setString(3, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRAC(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get first RAC failed: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Delete WL entry ───────────────────────────────────────────────────────
    public boolean deleteWL(int bookingId) throws DatabaseException {
        String sql = "DELETE FROM railway_waiting_list WHERE booking_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Delete WL failed: " + e.getMessage(), e);
        }
    }

    // ── Delete RAC entry ──────────────────────────────────────────────────────
    public boolean deleteRAC(int bookingId) throws DatabaseException {
        String sql = "DELETE FROM railway_rac WHERE booking_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Delete RAC failed: " + e.getMessage(), e);
        }
    }

    // ── Shift WL positions down by 1 after first is promoted ──────────────────
    public void shiftWLPositions(int trainId, int coachId, String date) throws DatabaseException {
        String sql = """
            UPDATE railway_waiting_list SET wl_position = wl_position - 1
            WHERE train_id=? AND coach_id=? AND journey_date=? AND wl_position > 1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId); ps.setInt(2, coachId); ps.setString(3, date);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Shift WL positions failed: " + e.getMessage(), e);
        }
    }

    // ── Get full WL list for a coach/date ─────────────────────────────────────
    public CustomLinkedList<railway.models.WaitingListEntry> getWLList(
            int trainId, int coachId, String date) throws DatabaseException {
        String sql = """
            SELECT w.*, p.name as passenger_name
            FROM railway_waiting_list w
            JOIN railway_bookings b ON w.booking_id = b.booking_id
            JOIN railway_passengers p ON p.booking_id = b.booking_id
            WHERE w.train_id=? AND w.coach_id=? AND w.journey_date=?
            ORDER BY w.wl_position
            """;
        CustomLinkedList<railway.models.WaitingListEntry> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId); ps.setInt(2, coachId); ps.setString(3, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    railway.models.WaitingListEntry e = mapWL(rs);
                    try { e.setPassengerName(rs.getString("passenger_name")); } catch (Exception ex) {}
                    list.add(e);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get WL list failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Mappers ───────────────────────────────────────────────────────────────
    private railway.models.WaitingListEntry mapWL(ResultSet rs) throws SQLException {
        railway.models.WaitingListEntry e = new railway.models.WaitingListEntry();
        e.setWlId       (rs.getInt("wl_id"));
        e.setBookingId  (rs.getInt("booking_id"));
        e.setTrainId    (rs.getInt("train_id"));
        e.setCoachId    (rs.getInt("coach_id"));
        e.setJourneyDate(rs.getString("journey_date"));
        e.setWlPosition (rs.getInt("wl_position"));
        e.setCreatedAt  (rs.getString("created_at"));
        return e;
    }

    private railway.models.RACEntry mapRAC(ResultSet rs) throws SQLException {
        railway.models.RACEntry e = new railway.models.RACEntry();
        e.setRacId      (rs.getInt("rac_id"));
        e.setBookingId  (rs.getInt("booking_id"));
        e.setTrainId    (rs.getInt("train_id"));
        e.setCoachId    (rs.getInt("coach_id"));
        e.setJourneyDate(rs.getString("journey_date"));
        e.setRacPosition(rs.getInt("rac_position"));
        e.setSeatNumber (rs.getString("seat_number"));
        e.setCreatedAt  (rs.getString("created_at"));
        return e;
    }
}