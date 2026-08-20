package railway.dao;

import common.database.DBConnection;
import common.exceptions.DatabaseException;
import datastructures.linkedlist.CustomLinkedList;
import java.sql.*;

public class BookingDAO {

    private final Connection conn;

    public BookingDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── Insert booking ────────────────────────────────────────────────────────
    public int insertBooking(String pnr, int userId, int trainId, int coachId,
                             String journeyDate, String status, double fare)
            throws DatabaseException {
        String sql = """
            INSERT INTO railway_bookings
              (pnr, user_id, train_id, coach_id, journey_date, status, total_fare)
            VALUES (?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, pnr);
            ps.setInt   (2, userId);
            ps.setInt   (3, trainId);
            ps.setInt   (4, coachId);
            ps.setString(5, journeyDate);
            ps.setString(6, status);
            ps.setDouble(7, fare);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Insert booking failed: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Insert passenger ──────────────────────────────────────────────────────
    public int insertPassenger(int bookingId, String name, int age, String gender,
                               String seat, String berth, String idType, String idNum)
            throws DatabaseException {
        String sql = """
            INSERT INTO railway_passengers
              (booking_id, name, age, gender, seat_number, berth_type, id_type, id_number)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, bookingId);
            ps.setString(2, name);
            ps.setInt   (3, age);
            ps.setString(4, gender);
            ps.setString(5, seat);
            ps.setString(6, berth);
            ps.setString(7, idType);
            ps.setString(8, idNum);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Insert passenger failed: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Get booking by PNR ────────────────────────────────────────────────────
    public railway.models.Booking getByPNR(String pnr) throws DatabaseException {
        String sql = """
            SELECT b.*, t.train_number, t.train_name, t.departure, t.arrival,
                   c.coach_name, c.coach_type, r.origin, r.destination
            FROM railway_bookings b
            JOIN railway_trains t  ON b.train_id  = t.train_id
            JOIN railway_coaches c ON b.coach_id  = c.coach_id
            JOIN railway_routes r  ON t.route_id  = r.route_id
            WHERE b.pnr = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapBooking(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get booking by PNR failed: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Get booking by ID ─────────────────────────────────────────────────────
    public railway.models.Booking getById(int bookingId) throws DatabaseException {
        String sql = """
            SELECT b.*, t.train_number, t.train_name, t.departure, t.arrival,
                   c.coach_name, c.coach_type, r.origin, r.destination
            FROM railway_bookings b
            JOIN railway_trains t  ON b.train_id  = t.train_id
            JOIN railway_coaches c ON b.coach_id  = c.coach_id
            JOIN railway_routes r  ON t.route_id  = r.route_id
            WHERE b.booking_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapBooking(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get booking by ID failed: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Get bookings by user ──────────────────────────────────────────────────
    public CustomLinkedList<railway.models.Booking> getByUser(int userId) throws DatabaseException {
        String sql = """
            SELECT b.*, t.train_number, t.train_name, t.departure, t.arrival,
                   c.coach_name, c.coach_type, r.origin, r.destination
            FROM railway_bookings b
            JOIN railway_trains t  ON b.train_id  = t.train_id
            JOIN railway_coaches c ON b.coach_id  = c.coach_id
            JOIN railway_routes r  ON t.route_id  = r.route_id
            WHERE b.user_id = ?
            ORDER BY b.booking_date DESC
            """;
        CustomLinkedList<railway.models.Booking> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBooking(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get bookings by user failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Get all bookings (admin) ─────────────────────────────────────────────
    public CustomLinkedList<railway.models.Booking> getAllBookings(String statusFilter)
            throws DatabaseException {
        String sql = """
            SELECT b.*, t.train_number, t.train_name, t.departure, t.arrival,
                   c.coach_name, c.coach_type, r.origin, r.destination
            FROM railway_bookings b
            JOIN railway_trains t  ON b.train_id  = t.train_id
            JOIN railway_coaches c ON b.coach_id  = c.coach_id
            JOIN railway_routes r  ON t.route_id  = r.route_id
            """
            + (statusFilter != null ? " WHERE b.status = ?" : "")
            + " ORDER BY b.booking_date DESC";
        CustomLinkedList<railway.models.Booking> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (statusFilter != null) ps.setString(1, statusFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBooking(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get all bookings failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Get passengers by booking ─────────────────────────────────────────────
    public CustomLinkedList<railway.models.Passenger> getPassengers(int bookingId)
            throws DatabaseException {
        String sql = "SELECT * FROM railway_passengers WHERE booking_id = ?";
        CustomLinkedList<railway.models.Passenger> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    railway.models.Passenger p = new railway.models.Passenger();
                    p.setPassengerId(rs.getInt("passenger_id"));
                    p.setBookingId  (rs.getInt("booking_id"));
                    p.setName       (rs.getString("name"));
                    p.setAge        (rs.getInt("age"));
                    p.setGender     (rs.getString("gender"));
                    p.setSeatNumber (rs.getString("seat_number"));
                    p.setBerthType  (rs.getString("berth_type"));
                    p.setIdType     (rs.getString("id_type"));
                    p.setIdNumber   (rs.getString("id_number"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get passengers failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Update booking status ─────────────────────────────────────────────────
    public boolean updateStatus(int bookingId, String status) throws DatabaseException {
        String sql = "UPDATE railway_bookings SET status = ? WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt   (2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Update booking status failed: " + e.getMessage(), e);
        }
    }

    // ── Update passenger seat ─────────────────────────────────────────────────
    public boolean updatePassengerSeat(int passengerId, String seat, String berth)
            throws DatabaseException {
        String sql = "UPDATE railway_passengers SET seat_number=?, berth_type=? WHERE passenger_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seat);
            ps.setString(2, berth);
            ps.setInt   (3, passengerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Update passenger seat failed: " + e.getMessage(), e);
        }
    }

    // ── Get booked seat numbers for a coach/date ───────────────────────────────
    public CustomLinkedList<String> getBookedSeats(int trainId, int coachId, String date)
            throws DatabaseException {
        String sql = """
            SELECT p.seat_number FROM railway_passengers p
            JOIN railway_bookings b ON p.booking_id = b.booking_id
            WHERE b.train_id=? AND b.coach_id=? AND b.journey_date=?
            AND b.status IN ('CONFIRMED','RAC')
            """;
        CustomLinkedList<String> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, trainId);
            ps.setInt   (2, coachId);
            ps.setString(3, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("seat_number"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get booked seats failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Count passengers by booking status for availability/RAC checks ───────
    public int countBookedPassengers(int trainId, int coachId, String date, String status)
            throws DatabaseException {
        String sql = """
            SELECT COUNT(*) FROM railway_passengers p
            JOIN railway_bookings b ON p.booking_id = b.booking_id
            WHERE b.train_id=? AND b.coach_id=? AND b.journey_date=? AND b.status=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId);
            ps.setInt(2, coachId);
            ps.setString(3, date);
            ps.setString(4, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Count booked passengers failed: " + e.getMessage(), e);
        }
        return 0;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private railway.models.Booking mapBooking(ResultSet rs) throws SQLException {
        railway.models.Booking b = new railway.models.Booking();
        b.setBookingId  (rs.getInt("booking_id"));
        b.setPnr        (rs.getString("pnr"));
        b.setUserId     (rs.getInt("user_id"));
        b.setTrainId    (rs.getInt("train_id"));
        b.setCoachId    (rs.getInt("coach_id"));
        b.setJourneyDate(rs.getString("journey_date"));
        b.setBookingDate(rs.getString("booking_date"));
        b.setStatus     (rs.getString("status"));
        b.setTotalFare  (rs.getDouble("total_fare"));
        b.setTrainNumber(rs.getString("train_number"));
        b.setTrainName  (rs.getString("train_name"));
        b.setCoachName  (rs.getString("coach_name"));
        b.setCoachType  (rs.getString("coach_type"));
        b.setOrigin     (rs.getString("origin"));
        b.setDestination(rs.getString("destination"));
        b.setDeparture  (rs.getString("departure"));
        b.setArrival    (rs.getString("arrival"));
        return b;
    }
}
