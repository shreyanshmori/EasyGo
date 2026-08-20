package bus.dao;

import common.database.DBConnection;
import common.exceptions.DatabaseException;
import datastructures.linkedlist.CustomLinkedList;
import bus.models.BusBooking;
import bus.models.BusPassenger;
import java.sql.*;

public class BusBookingDAO {

    private final Connection conn;

    public BusBookingDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── Insert booking ────────────────────────────────────────────────────────
    public int insertBooking(String ref, int userId, int busId,
                             String date, String status, double fare)
            throws DatabaseException {
        String sql = """
            INSERT INTO bus_bookings (booking_ref,user_id,bus_id,journey_date,status,total_fare)
            VALUES(?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,ref); ps.setInt(2,userId); ps.setInt(3,busId);
            ps.setString(4,date); ps.setString(5,status); ps.setDouble(6,fare);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Insert booking: " + e.getMessage(), e); }
        return -1;
    }

    // ── Insert passenger ──────────────────────────────────────────────────────
    public int insertPassenger(int bookingId, String name, int age, String gender,
                               String seat, String idType, String idNum)
            throws DatabaseException {
        String sql = """
            INSERT INTO bus_passengers (booking_id,name,age,gender,seat_number,id_type,id_number)
            VALUES(?,?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,bookingId); ps.setString(2,name); ps.setInt(3,age);
            ps.setString(4,gender); ps.setString(5,seat);
            ps.setString(6,idType); ps.setString(7,idNum);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Insert passenger: " + e.getMessage(), e); }
        return -1;
    }

    // ── Insert payment ────────────────────────────────────────────────────────
    public void insertPayment(int bookingId, double amount, String method,
                              String status, String txnId) throws DatabaseException {
        String sql = """
            INSERT INTO bus_payments (booking_id,amount,payment_method,status,transaction_id)
            VALUES(?,?,?,?,?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,bookingId); ps.setDouble(2,amount);
            ps.setString(3,method); ps.setString(4,status); ps.setString(5,txnId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Insert payment: " + e.getMessage(), e); }
    }

    // ── Get booking by reference ──────────────────────────────────────────────
    public BusBooking getByRef(String ref) throws DatabaseException {
        String sql = """
            SELECT bk.*, bd.bus_number, bd.bus_name, bd.bus_type,
                   bd.departure, bd.arrival,
                   r.origin, r.destination, r.distance_km
            FROM bus_bookings bk
            JOIN bus_details bd ON bk.bus_id=bd.bus_id
            JOIN bus_routes  r  ON bd.route_id=r.route_id
            WHERE bk.booking_ref=?""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ref);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapBooking(rs); }
        } catch (SQLException e) { throw new DatabaseException("Get by ref: " + e.getMessage(), e); }
        return null;
    }

    // ── Get bookings by user ──────────────────────────────────────────────────
    public CustomLinkedList<BusBooking> getByUser(int userId) throws DatabaseException {
        String sql = """
            SELECT bk.*, bd.bus_number, bd.bus_name, bd.bus_type,
                   bd.departure, bd.arrival,
                   r.origin, r.destination, r.distance_km
            FROM bus_bookings bk
            JOIN bus_details bd ON bk.bus_id=bd.bus_id
            JOIN bus_routes  r  ON bd.route_id=r.route_id
            WHERE bk.user_id=? ORDER BY bk.booking_date DESC""";
        CustomLinkedList<BusBooking> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapBooking(rs)); }
        } catch (SQLException e) { throw new DatabaseException("Get by user: " + e.getMessage(), e); }
        return list;
    }

    // ── Get all bookings (admin) ──────────────────────────────────────────────
    public CustomLinkedList<BusBooking> getAllBookings(String statusFilter)
            throws DatabaseException {
        String sql = """
            SELECT bk.*, bd.bus_number, bd.bus_name, bd.bus_type,
                   bd.departure, bd.arrival,
                   r.origin, r.destination, r.distance_km
            FROM bus_bookings bk
            JOIN bus_details bd ON bk.bus_id=bd.bus_id
            JOIN bus_routes  r  ON bd.route_id=r.route_id"""
            + (statusFilter != null ? " WHERE bk.status='" + statusFilter + "'" : "")
            + " ORDER BY bk.booking_date DESC";
        CustomLinkedList<BusBooking> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapBooking(rs)); }
        } catch (SQLException e) { throw new DatabaseException("Get all: " + e.getMessage(), e); }
        return list;
    }

    // ── Get passengers by booking ─────────────────────────────────────────────
    public CustomLinkedList<BusPassenger> getPassengers(int bookingId)
            throws DatabaseException {
        String sql = "SELECT * FROM bus_passengers WHERE booking_id=?";
        CustomLinkedList<BusPassenger> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BusPassenger p = new BusPassenger();
                    p.setPassengerId(rs.getInt("passenger_id"));
                    p.setBookingId  (rs.getInt("booking_id"));
                    p.setName       (rs.getString("name"));
                    p.setAge        (rs.getInt("age"));
                    p.setGender     (rs.getString("gender"));
                    p.setSeatNumber (rs.getString("seat_number"));
                    p.setIdType     (rs.getString("id_type"));
                    p.setIdNumber   (rs.getString("id_number"));
                    list.add(p);
                }
            }
        } catch (SQLException e) { throw new DatabaseException("Get passengers: " + e.getMessage(), e); }
        return list;
    }

    // ── Update booking status ─────────────────────────────────────────────────
    public boolean updateStatus(int bookingId, String status) throws DatabaseException {
        String sql = "UPDATE bus_bookings SET status=? WHERE booking_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,status); ps.setInt(2,bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DatabaseException("Update status: " + e.getMessage(), e); }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private BusBooking mapBooking(ResultSet rs) throws SQLException {
        BusBooking b = new BusBooking();
        b.setBookingId  (rs.getInt("booking_id"));
        b.setBookingRef (rs.getString("booking_ref"));
        b.setUserId     (rs.getInt("user_id"));
        b.setBusId      (rs.getInt("bus_id"));
        b.setJourneyDate(rs.getString("journey_date"));
        b.setBookingDate(rs.getString("booking_date"));
        b.setStatus     (rs.getString("status"));
        b.setTotalFare  (rs.getDouble("total_fare"));
        b.setBusNumber  (rs.getString("bus_number"));
        b.setBusName    (rs.getString("bus_name"));
        b.setBusType    (rs.getString("bus_type"));
        b.setOrigin     (rs.getString("origin"));
        b.setDestination(rs.getString("destination"));
        b.setDeparture  (rs.getString("departure"));
        b.setArrival    (rs.getString("arrival"));
        b.setDistanceKm (rs.getDouble("distance_km"));
        return b;
    }
}
