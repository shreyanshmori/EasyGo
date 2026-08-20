package bus.dao;

import common.database.DBConnection;
import common.exceptions.DatabaseException;
import datastructures.linkedlist.CustomLinkedList;
import bus.models.*;

import java.sql.*;

public class BusDAO {

    private final Connection conn;

    public BusDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── Insert route ──────────────────────────────────────────────────────────
    public int insertRoute(String origin, String dest, double km) throws DatabaseException {
        String sql = "INSERT OR IGNORE INTO bus_routes (origin,destination,distance_km) VALUES(?,?,?)";
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
        String sql = "SELECT route_id FROM bus_routes WHERE origin=? AND destination=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, origin.toUpperCase()); ps.setString(2, dest.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Get route id: " + e.getMessage(), e); }
        return -1;
    }

    // ── Insert bus ────────────────────────────────────────────────────────────
    public int insertBus(String number, String name, int routeId, String type,
                         String dep, String arr, int seats, double farePerKm)
            throws DatabaseException {
        String sql = """
            INSERT INTO bus_details
              (bus_number,bus_name,route_id,bus_type,departure,arrival,total_seats,fare_per_km)
            VALUES(?,?,?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,number); ps.setString(2,name);   ps.setInt(3,routeId);
            ps.setString(4,type);   ps.setString(5,dep);    ps.setString(6,arr);
            ps.setInt(7,seats);     ps.setDouble(8,farePerKm);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Insert bus: " + e.getMessage(), e); }
        return -1;
    }

    // ── Generate seats for a bus ──────────────────────────────────────────────
    public void generateSeats(int busId, String busType, int totalSeats)
            throws DatabaseException {
        String sql = "INSERT OR IGNORE INTO bus_seats (bus_id,seat_number,seat_type) VALUES(?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            boolean isSleeper = busType.toUpperCase().contains("SLEEPER");

            if (isSleeper) {
                // Sleeper: rows of 2 lower + 2 upper per bay
                int bays = totalSeats / 4;
                for (int bay = 1; bay <= bays; bay++) {
                    ps.setInt(1, busId); ps.setString(2, "L" + bay + "A"); ps.setString(3, "LOWER"); ps.addBatch();
                    ps.setInt(1, busId); ps.setString(2, "L" + bay + "B"); ps.setString(3, "LOWER"); ps.addBatch();
                    ps.setInt(1, busId); ps.setString(2, "U" + bay + "A"); ps.setString(3, "UPPER"); ps.addBatch();
                    ps.setInt(1, busId); ps.setString(2, "U" + bay + "B"); ps.setString(3, "UPPER"); ps.addBatch();
                }
                // Extra seats if not divisible by 4
                int extra = totalSeats % 4;
                for (int i = 0; i < extra; i++) {
                    ps.setInt(1, busId); ps.setString(2, "L" + (bays+1) + (char)('A'+i));
                    ps.setString(3, "LOWER"); ps.addBatch();
                }
            } else {
                // Seater: rows of 2+2 (Window/Aisle)
                int rows = totalSeats / 4;
                for (int row = 1; row <= rows; row++) {
                    ps.setInt(1,busId); ps.setString(2,"R"+row+"A"); ps.setString(3,"WINDOW"); ps.addBatch();
                    ps.setInt(1,busId); ps.setString(2,"R"+row+"B"); ps.setString(3,"AISLE");  ps.addBatch();
                    ps.setInt(1,busId); ps.setString(2,"R"+row+"C"); ps.setString(3,"AISLE");  ps.addBatch();
                    ps.setInt(1,busId); ps.setString(2,"R"+row+"D"); ps.setString(3,"WINDOW"); ps.addBatch();
                }
                int extra = totalSeats % 4;
                for (int i = 0; i < extra; i++) {
                    ps.setInt(1,busId); ps.setString(2,"R"+(rows+1)+(char)('A'+i));
                    ps.setString(3, i==0||i==3 ? "WINDOW" : "AISLE"); ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (SQLException e) { throw new DatabaseException("Generate seats: " + e.getMessage(), e); }
    }

    // ── Search buses by route ─────────────────────────────────────────────────
    public CustomLinkedList<Bus> searchBuses(String origin, String dest)
            throws DatabaseException {
        String sql = """
            SELECT b.*, r.origin, r.destination, r.distance_km
            FROM bus_details b JOIN bus_routes r ON b.route_id=r.route_id
            WHERE UPPER(r.origin)=? AND UPPER(r.destination)=? AND b.status='ACTIVE'
            ORDER BY b.departure""";
        CustomLinkedList<Bus> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,origin.toUpperCase()); ps.setString(2,dest.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapBus(rs)); }
        } catch (SQLException e) { throw new DatabaseException("Search buses: " + e.getMessage(), e); }
        return list;
    }

    // ── Get bus by ID ─────────────────────────────────────────────────────────
    public Bus getById(int busId) throws DatabaseException {
        String sql = """
            SELECT b.*, r.origin, r.destination, r.distance_km
            FROM bus_details b JOIN bus_routes r ON b.route_id=r.route_id
            WHERE b.bus_id=?""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, busId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapBus(rs); }
        } catch (SQLException e) { throw new DatabaseException("Get bus by id: " + e.getMessage(), e); }
        return null;
    }

    // ── Get bus by number ─────────────────────────────────────────────────────
    public Bus getByNumber(String number) throws DatabaseException {
        String sql = """
            SELECT b.*, r.origin, r.destination, r.distance_km
            FROM bus_details b JOIN bus_routes r ON b.route_id=r.route_id
            WHERE b.bus_number=?""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapBus(rs); }
        } catch (SQLException e) { throw new DatabaseException("Get bus by number: " + e.getMessage(), e); }
        return null;
    }

    // ── Get all buses ─────────────────────────────────────────────────────────
    public CustomLinkedList<Bus> getAllBuses() throws DatabaseException {
        String sql = """
            SELECT b.*, r.origin, r.destination, r.distance_km
            FROM bus_details b JOIN bus_routes r ON b.route_id=r.route_id
            ORDER BY b.bus_number""";
        CustomLinkedList<Bus> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapBus(rs)); }
        } catch (SQLException e) { throw new DatabaseException("Get all buses: " + e.getMessage(), e); }
        return list;
    }

    // ── Get all seats for a bus ───────────────────────────────────────────────
    public CustomLinkedList<BusSeat> getSeats(int busId) throws DatabaseException {
        String sql = "SELECT * FROM bus_seats WHERE bus_id=? ORDER BY seat_number";
        CustomLinkedList<BusSeat> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, busId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new BusSeat(
                    rs.getInt("seat_id"), rs.getInt("bus_id"),
                    rs.getString("seat_number"), rs.getString("seat_type"),
                    rs.getInt("is_booked") == 1));
            }
        } catch (SQLException e) { throw new DatabaseException("Get seats: " + e.getMessage(), e); }
        return list;
    }

    // ── Get booked seat numbers for date ─────────────────────────────────────
    public CustomLinkedList<String> getBookedSeats(int busId, String date)
            throws DatabaseException {
        String sql = """
            SELECT p.seat_number FROM bus_passengers p
            JOIN bus_bookings b ON p.booking_id=b.booking_id
            WHERE b.bus_id=? AND b.journey_date=? AND b.status='CONFIRMED'""";
        CustomLinkedList<String> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,busId); ps.setString(2,date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("seat_number"));
            }
        } catch (SQLException e) { throw new DatabaseException("Get booked seats: " + e.getMessage(), e); }
        return list;
    }

    // ── Count booked seats ────────────────────────────────────────────────────
    public int countBookedSeats(int busId, String date) throws DatabaseException {
        String sql = """
            SELECT COUNT(*) FROM bus_passengers p
            JOIN bus_bookings b ON p.booking_id=b.booking_id
            WHERE b.bus_id=? AND b.journey_date=? AND b.status='CONFIRMED'""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,busId); ps.setString(2,date);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("Count booked: " + e.getMessage(), e); }
        return 0;
    }

    // ── Update bus status ─────────────────────────────────────────────────────
    public boolean updateStatus(int busId, String status) throws DatabaseException {
        String sql = "UPDATE bus_details SET status=? WHERE bus_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,status); ps.setInt(2,busId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DatabaseException("Update status: " + e.getMessage(), e); }
    }

    // ── Get all routes ────────────────────────────────────────────────────────
    public CustomLinkedList<BusRoute> getAllRoutes() throws DatabaseException {
        String sql = "SELECT * FROM bus_routes ORDER BY origin";
        CustomLinkedList<BusRoute> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new BusRoute(
                    rs.getInt("route_id"), rs.getString("origin"),
                    rs.getString("destination"), rs.getDouble("distance_km")));
            }
        } catch (SQLException e) { throw new DatabaseException("Get routes: " + e.getMessage(), e); }
        return list;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private Bus mapBus(ResultSet rs) throws SQLException {
        Bus b = new Bus(rs.getInt("bus_id"), rs.getString("bus_number"),
            rs.getString("bus_name"), rs.getInt("route_id"),
            rs.getString("bus_type"), rs.getString("departure"),
            rs.getString("arrival"), rs.getInt("total_seats"),
            rs.getDouble("fare_per_km"), rs.getString("status"));
        b.setOrigin(rs.getString("origin"));
        b.setDestination(rs.getString("destination"));
        b.setDistanceKm(rs.getDouble("distance_km"));
        return b;
    }
}

