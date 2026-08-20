package flight.dao;

import common.database.DBConnection;
import common.exceptions.DatabaseException;
import datastructures.linkedlist.CustomLinkedList;
import flight.models.FlightBooking;
import flight.models.FlightPassenger;
import java.sql.*;

public class FlightBookingDAO {

    private final Connection conn;

    public FlightBookingDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── Insert booking ────────────────────────────────────────────────────────
    public int insertBooking(String ref, int userId, int flightId,
                             String date, String classType,
                             String status, double fare) throws DatabaseException {
        String sql = """
            INSERT INTO flight_bookings
              (booking_ref,user_id,flight_id,journey_date,class_type,status,total_fare)
            VALUES(?,?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,ref); ps.setInt(2,userId); ps.setInt(3,flightId);
            ps.setString(4,date); ps.setString(5,classType);
            ps.setString(6,status); ps.setDouble(7,fare);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Insert booking: " + e.getMessage(), e); }
        return -1;
    }

    // ── Insert passenger ──────────────────────────────────────────────────────
    public int insertPassenger(int bookingId, String name, int age, String gender,
                               String seat, String passport, String nationality)
            throws DatabaseException {
        String sql = """
            INSERT INTO flight_passengers
              (booking_id,name,age,gender,seat_number,passport_no,nationality)
            VALUES(?,?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,bookingId); ps.setString(2,name); ps.setInt(3,age);
            ps.setString(4,gender); ps.setString(5,seat);
            ps.setString(6,passport); ps.setString(7,nationality);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Insert passenger: " + e.getMessage(), e); }
        return -1;
    }

    // ── Insert meal ───────────────────────────────────────────────────────────
    public void insertMeal(int passengerId, int bookingId, String type, double charge)
            throws DatabaseException {
        String sql = "INSERT INTO flight_meals (passenger_id,booking_id,meal_type,meal_charge) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,passengerId); ps.setInt(2,bookingId);
            ps.setString(3,type); ps.setDouble(4,charge);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Insert meal: " + e.getMessage(), e); }
    }

    // ── Insert luggage ────────────────────────────────────────────────────────
    public void insertLuggage(int passengerId, int bookingId, double kg, double extra)
            throws DatabaseException {
        String sql = "INSERT INTO flight_luggage (passenger_id,booking_id,weight_kg,extra_charge) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,passengerId); ps.setInt(2,bookingId);
            ps.setDouble(3,kg); ps.setDouble(4,extra);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Insert luggage: " + e.getMessage(), e); }
    }

    // ── Get booking by reference ──────────────────────────────────────────────
    public FlightBooking getByRef(String ref) throws DatabaseException {
        String sql = """
            SELECT b.*, f.flight_number, f.airline, f.departure, f.arrival,
                   r.origin, r.destination
            FROM flight_bookings b
            JOIN flight_details f ON b.flight_id=f.flight_id
            JOIN flight_routes  r ON f.route_id=r.route_id
            WHERE b.booking_ref=?""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ref);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapBooking(rs); }
        } catch (SQLException e) { throw new DatabaseException("Get by ref: " + e.getMessage(), e); }
        return null;
    }

    // ── Get bookings by user ──────────────────────────────────────────────────
    public CustomLinkedList<FlightBooking> getByUser(int userId) throws DatabaseException {
        String sql = """
            SELECT b.*, f.flight_number, f.airline, f.departure, f.arrival,
                   r.origin, r.destination
            FROM flight_bookings b
            JOIN flight_details f ON b.flight_id=f.flight_id
            JOIN flight_routes  r ON f.route_id=r.route_id
            WHERE b.user_id=? ORDER BY b.booking_date DESC""";
        CustomLinkedList<FlightBooking> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapBooking(rs)); }
        } catch (SQLException e) { throw new DatabaseException("Get by user: " + e.getMessage(), e); }
        return list;
    }

    // ── Get all bookings (admin) ──────────────────────────────────────────────
    public CustomLinkedList<FlightBooking> getAllBookings(String statusFilter)
            throws DatabaseException {
        String sql = """
            SELECT b.*, f.flight_number, f.airline, f.departure, f.arrival,
                   r.origin, r.destination
            FROM flight_bookings b
            JOIN flight_details f ON b.flight_id=f.flight_id
            JOIN flight_routes  r ON f.route_id=r.route_id"""
            + (statusFilter != null ? " WHERE b.status='" + statusFilter + "'" : "")
            + " ORDER BY b.booking_date DESC";
        CustomLinkedList<FlightBooking> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapBooking(rs)); }
        } catch (SQLException e) { throw new DatabaseException("Get all: " + e.getMessage(), e); }
        return list;
    }

    // ── Get passengers by booking ─────────────────────────────────────────────
    public CustomLinkedList<FlightPassenger> getPassengers(int bookingId) throws DatabaseException {
        String sql = "SELECT * FROM flight_passengers WHERE booking_id=?";
        CustomLinkedList<FlightPassenger> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FlightPassenger p = new FlightPassenger();
                    p.setPassengerId(rs.getInt("passenger_id"));
                    p.setBookingId  (rs.getInt("booking_id"));
                    p.setName       (rs.getString("name"));
                    p.setAge        (rs.getInt("age"));
                    p.setGender     (rs.getString("gender"));
                    p.setSeatNumber (rs.getString("seat_number"));
                    p.setPassportNo (rs.getString("passport_no"));
                    p.setNationality(rs.getString("nationality"));
                    list.add(p);
                }
            }
        } catch (SQLException e) { throw new DatabaseException("Get passengers: " + e.getMessage(), e); }
        return list;
    }

    // ── Update booking status ─────────────────────────────────────────────────
    public boolean updateStatus(int bookingId, String status) throws DatabaseException {
        String sql = "UPDATE flight_bookings SET status=? WHERE booking_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,status); ps.setInt(2,bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DatabaseException("Update status: " + e.getMessage(), e); }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private FlightBooking mapBooking(ResultSet rs) throws SQLException {
        FlightBooking b = new FlightBooking();
        b.setBookingId  (rs.getInt("booking_id"));
        b.setBookingRef (rs.getString("booking_ref"));
        b.setUserId     (rs.getInt("user_id"));
        b.setFlightId   (rs.getInt("flight_id"));
        b.setJourneyDate(rs.getString("journey_date"));
        b.setClassType  (rs.getString("class_type"));
        b.setBookingDate(rs.getString("booking_date"));
        b.setStatus     (rs.getString("status"));
        b.setTotalFare  (rs.getDouble("total_fare"));
        b.setFlightNumber(rs.getString("flight_number"));
        b.setAirline    (rs.getString("airline"));
        b.setOrigin     (rs.getString("origin"));
        b.setDestination(rs.getString("destination"));
        b.setDeparture  (rs.getString("departure"));
        b.setArrival    (rs.getString("arrival"));
        return b;
    }
}
