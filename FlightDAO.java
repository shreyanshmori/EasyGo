package flight.dao;

import common.database.DBConnection;
import common.exceptions.DatabaseException;
import datastructures.linkedlist.CustomLinkedList;
import flight.models.*;

import java.sql.*;

public class FlightDAO {

    private final Connection conn;

    public FlightDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── Insert route ──────────────────────────────────────────────────────────
    public int insertRoute(String origin, String dest, double km) throws DatabaseException {
        String sql = "INSERT OR IGNORE INTO flight_routes (origin,destination,distance_km) VALUES(?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, origin.toUpperCase());
            ps.setString(2, dest.toUpperCase());
            ps.setDouble(3, km);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
            return getRouteId(origin, dest);
        } catch (SQLException e) { throw new DatabaseException("Insert route: " + e.getMessage(), e); }
    }

    public int getRouteId(String origin, String dest) throws DatabaseException {
        String sql = "SELECT route_id FROM flight_routes WHERE origin=? AND destination=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, origin.toUpperCase()); ps.setString(2, dest.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Get route id: " + e.getMessage(), e); }
        return -1;
    }

    // ── Insert flight ─────────────────────────────────────────────────────────
    public int insertFlight(String number, String airline, int routeId,
                            String dep, String arr, int seats) throws DatabaseException {
        String sql = """
            INSERT INTO flight_details (flight_number,airline,route_id,departure,arrival,total_seats)
            VALUES(?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,number); ps.setString(2,airline); ps.setInt(3,routeId);
            ps.setString(4,dep);    ps.setString(5,arr);      ps.setInt(6,seats);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Insert flight: " + e.getMessage(), e); }
        return -1;
    }

    // ── Insert seat ───────────────────────────────────────────────────────────
    public void insertSeat(int flightId, String seatNo, String classType) throws DatabaseException {
        String sql = "INSERT OR IGNORE INTO flight_seats (flight_id,seat_number,class_type) VALUES(?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,flightId); ps.setString(2,seatNo); ps.setString(3,classType);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Insert seat: " + e.getMessage(), e); }
    }

    // ── Bulk seat generation for a flight ─────────────────────────────────────
    public void generateSeats(int flightId, int economyCount,
                               int businessCount, int firstCount) throws DatabaseException {
        // Economy: rows 10-49, A-F
        int row = 10;
        for (int i = 0; i < economyCount; i++) {
            char col = (char)('A' + (i % 6));
            if (i > 0 && i % 6 == 0) row++;
            insertSeat(flightId, row + String.valueOf(col), "ECONOMY");
        }
        // Business: rows 3-9, A-D
        row = 3;
        for (int i = 0; i < businessCount; i++) {
            char col = (char)('A' + (i % 4));
            if (i > 0 && i % 4 == 0) row++;
            insertSeat(flightId, row + String.valueOf(col), "BUSINESS");
        }
        // First: rows 1-2, A-D
        row = 1;
        for (int i = 0; i < firstCount; i++) {
            char col = (char)('A' + (i % 4));
            if (i > 0 && i % 4 == 0) row++;
            insertSeat(flightId, row + String.valueOf(col), "FIRST");
        }
    }

    // ── Search flights by route ───────────────────────────────────────────────
    public CustomLinkedList<Flight> searchFlights(String origin, String dest)
            throws DatabaseException {
        String sql = """
            SELECT f.*, r.origin, r.destination, r.distance_km
            FROM flight_details f JOIN flight_routes r ON f.route_id=r.route_id
            WHERE UPPER(r.origin)=? AND UPPER(r.destination)=? AND f.status='SCHEDULED'
            ORDER BY f.departure""";
        CustomLinkedList<Flight> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, origin.toUpperCase()); ps.setString(2, dest.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapFlight(rs)); }
        } catch (SQLException e) { throw new DatabaseException("Search flights: " + e.getMessage(), e); }
        return list;
    }

    // ── Get flight by ID ──────────────────────────────────────────────────────
    public Flight getById(int flightId) throws DatabaseException {
        String sql = """
            SELECT f.*, r.origin, r.destination, r.distance_km
            FROM flight_details f JOIN flight_routes r ON f.route_id=r.route_id
            WHERE f.flight_id=?""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, flightId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapFlight(rs); }
        } catch (SQLException e) { throw new DatabaseException("Get flight: " + e.getMessage(), e); }
        return null;
    }

    // ── Get flight by number ──────────────────────────────────────────────────
    public Flight getByNumber(String number) throws DatabaseException {
        String sql = """
            SELECT f.*, r.origin, r.destination, r.distance_km
            FROM flight_details f JOIN flight_routes r ON f.route_id=r.route_id
            WHERE f.flight_number=?""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapFlight(rs); }
        } catch (SQLException e) { throw new DatabaseException("Get flight by no: " + e.getMessage(), e); }
        return null;
    }

    // ── Get all flights ───────────────────────────────────────────────────────
    public CustomLinkedList<Flight> getAllFlights() throws DatabaseException {
        String sql = """
            SELECT f.*, r.origin, r.destination, r.distance_km
            FROM flight_details f JOIN flight_routes r ON f.route_id=r.route_id
            ORDER BY f.flight_number""";
        CustomLinkedList<Flight> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapFlight(rs)); }
        } catch (SQLException e) { throw new DatabaseException("Get all flights: " + e.getMessage(), e); }
        return list;
    }

    // ── Count booked seats by class ───────────────────────────────────────────
    public int countBookedByClass(int flightId, String classType, String date)
            throws DatabaseException {
        String sql = """
            SELECT COUNT(*) FROM flight_passengers p
            JOIN flight_bookings b ON p.booking_id=b.booking_id
            WHERE b.flight_id=? AND b.class_type=? AND b.journey_date=?
            AND b.status='CONFIRMED'""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,flightId); ps.setString(2,classType); ps.setString(3,date);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Count booked: " + e.getMessage(), e); }
        return 0;
    }

    // ── Get available seat for class ──────────────────────────────────────────
    public String getAvailableSeat(int flightId, String classType, String date)
            throws DatabaseException {
        String sql = """
            SELECT s.seat_number FROM flight_seats s
            WHERE s.flight_id=? AND s.class_type=?
            AND s.seat_number NOT IN (
                SELECT p.seat_number FROM flight_passengers p
                JOIN flight_bookings b ON p.booking_id=b.booking_id
                WHERE b.flight_id=? AND b.journey_date=? AND b.status='CONFIRMED'
                AND p.seat_number IS NOT NULL
            )
            ORDER BY s.seat_number LIMIT 1""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,flightId); ps.setString(2,classType);
            ps.setInt(3,flightId); ps.setString(4,date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("seat_number");
            }
        } catch (SQLException e) { throw new DatabaseException("Get available seat: " + e.getMessage(), e); }
        return null;
    }

    // ── Count total seats by class ────────────────────────────────────────────
    public int countSeatsByClass(int flightId, String classType) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM flight_seats WHERE flight_id=? AND class_type=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,flightId); ps.setString(2,classType);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Count seats: " + e.getMessage(), e); }
        return 0;
    }

    // ── Update flight status ──────────────────────────────────────────────────
    public boolean updateStatus(int flightId, String status) throws DatabaseException {
        String sql = "UPDATE flight_details SET status=? WHERE flight_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,status); ps.setInt(2,flightId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DatabaseException("Update status: " + e.getMessage(), e); }
    }

    // ── Get all routes ────────────────────────────────────────────────────────
    public CustomLinkedList<FlightRoute> getAllRoutes() throws DatabaseException {
        String sql = "SELECT * FROM flight_routes ORDER BY origin";
        CustomLinkedList<FlightRoute> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new FlightRoute(
                    rs.getInt("route_id"), rs.getString("origin"),
                    rs.getString("destination"), rs.getDouble("distance_km")));
            }
        } catch (SQLException e) { throw new DatabaseException("Get routes: " + e.getMessage(), e); }
        return list;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private Flight mapFlight(ResultSet rs) throws SQLException {
        Flight f = new Flight(rs.getInt("flight_id"), rs.getString("flight_number"),
            rs.getString("airline"), rs.getInt("route_id"),
            rs.getString("departure"), rs.getString("arrival"),
            rs.getInt("total_seats"), rs.getString("status"));
        f.setOrigin(rs.getString("origin"));
        f.setDestination(rs.getString("destination"));
        f.setDistanceKm(rs.getDouble("distance_km"));
        return f;
    }
}


