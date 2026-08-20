package railway.dao;

import common.database.DBConnection;
import common.exceptions.DatabaseException;
import datastructures.linkedlist.CustomLinkedList;
import railway.models.Train;
import railway.models.Coach;
import railway.models.Route;

import java.sql.*;

public class TrainDAO {

    private final Connection conn;

    public TrainDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── Insert route ──────────────────────────────────────────────────────────
    public int insertRoute(String origin, String destination, double distanceKm)
            throws DatabaseException {
        String sql = "INSERT OR IGNORE INTO railway_routes (origin, destination, distance_km) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, origin.toUpperCase());
            ps.setString(2, destination.toUpperCase());
            ps.setDouble(3, distanceKm);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            // already exists — fetch id
            return getRouteId(origin, destination);
        } catch (SQLException e) {
            throw new DatabaseException("Insert route failed: " + e.getMessage(), e);
        }
    }

    public int getRouteId(String origin, String destination) throws DatabaseException {
        String sql = "SELECT route_id FROM railway_routes WHERE origin=? AND destination=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, origin.toUpperCase());
            ps.setString(2, destination.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get route id failed: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Insert train ──────────────────────────────────────────────────────────
    public int insertTrain(String number, String name, int routeId,
                           String dep, String arr, int seats) throws DatabaseException {
        String sql = """
            INSERT INTO railway_trains (train_number, train_name, route_id, departure, arrival, total_seats)
            VALUES (?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, number);
            ps.setString(2, name);
            ps.setInt   (3, routeId);
            ps.setString(4, dep);
            ps.setString(5, arr);
            ps.setInt   (6, seats);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Insert train failed: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Insert coach ──────────────────────────────────────────────────────────
    public int insertCoach(int trainId, String name, String type,
                           int seats, double farePerKm) throws DatabaseException {
        String sql = """
            INSERT INTO railway_coaches (train_id, coach_name, coach_type, total_seats, fare_per_km)
            VALUES (?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, trainId);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setInt   (4, seats);
            ps.setDouble(5, farePerKm);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Insert coach failed: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Search trains by origin & destination ─────────────────────────────────
    public CustomLinkedList<Train> searchTrains(String origin, String destination)
            throws DatabaseException {
        String sql = """
            SELECT t.*, r.origin, r.destination, r.distance_km
            FROM railway_trains t
            JOIN railway_routes r ON t.route_id = r.route_id
            WHERE UPPER(r.origin) = ? AND UPPER(r.destination) = ?
            AND t.status = 'ACTIVE'
            """;
        CustomLinkedList<Train> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, origin.toUpperCase());
            ps.setString(2, destination.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTrain(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Search trains failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Get train by ID ───────────────────────────────────────────────────────
    public Train getTrainById(int trainId) throws DatabaseException {
        String sql = """
            SELECT t.*, r.origin, r.destination, r.distance_km
            FROM railway_trains t
            JOIN railway_routes r ON t.route_id = r.route_id
            WHERE t.train_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapTrain(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get train failed: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Get train by number ───────────────────────────────────────────────────
    public Train getTrainByNumber(String number) throws DatabaseException {
        String sql = """
            SELECT t.*, r.origin, r.destination, r.distance_km
            FROM railway_trains t
            JOIN railway_routes r ON t.route_id = r.route_id
            WHERE t.train_number = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapTrain(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get train by number failed: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Get all trains ────────────────────────────────────────────────────────
    public CustomLinkedList<Train> getAllTrains() throws DatabaseException {
        String sql = """
            SELECT t.*, r.origin, r.destination, r.distance_km
            FROM railway_trains t
            JOIN railway_routes r ON t.route_id = r.route_id
            ORDER BY t.train_number
            """;
        CustomLinkedList<Train> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTrain(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get all trains failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Get coaches by train ──────────────────────────────────────────────────
    public CustomLinkedList<Coach> getCoachesByTrain(int trainId) throws DatabaseException {
        String sql = "SELECT * FROM railway_coaches WHERE train_id = ? ORDER BY coach_name";
        CustomLinkedList<Coach> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCoach(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get coaches failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Get coach by ID ───────────────────────────────────────────────────────
    public Coach getCoachById(int coachId) throws DatabaseException {
        String sql = "SELECT * FROM railway_coaches WHERE coach_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coachId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapCoach(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get coach by id failed: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Count booked seats for a coach on a date ──────────────────────────────
    public int countBookedSeats(int trainId, int coachId, String journeyDate)
            throws DatabaseException {
        String sql = """
            SELECT COUNT(*) FROM railway_passengers p
            JOIN railway_bookings b ON p.booking_id = b.booking_id
            WHERE b.train_id = ? AND b.coach_id = ?
            AND b.journey_date = ? AND b.status IN ('CONFIRMED','RAC')
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, trainId);
            ps.setInt   (2, coachId);
            ps.setString(3, journeyDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Count booked seats failed: " + e.getMessage(), e);
        }
        return 0;
    }

    // ── Get all routes ────────────────────────────────────────────────────────
    public CustomLinkedList<Route> getAllRoutes() throws DatabaseException {
        String sql = "SELECT * FROM railway_routes ORDER BY origin";
        CustomLinkedList<Route> list = new CustomLinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Route r = new Route(rs.getInt("route_id"), rs.getString("origin"),
                                        rs.getString("destination"), rs.getDouble("distance_km"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Get all routes failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Update train status ───────────────────────────────────────────────────
    public boolean updateTrainStatus(int trainId, String status) throws DatabaseException {
        String sql = "UPDATE railway_trains SET status = ? WHERE train_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt   (2, trainId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Update train status failed: " + e.getMessage(), e);
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────
    private Train mapTrain(ResultSet rs) throws SQLException {
        Train t = new Train(
            rs.getInt("train_id"), rs.getString("train_number"),
            rs.getString("train_name"), rs.getInt("route_id"),
            rs.getString("departure"), rs.getString("arrival"),
            rs.getInt("total_seats"), rs.getString("status")
        );
        t.setOrigin(rs.getString("origin"));
        t.setDestination(rs.getString("destination"));
        t.setDistanceKm(rs.getDouble("distance_km"));
        return t;
    }

    private Coach mapCoach(ResultSet rs) throws SQLException {
        return new Coach(
            rs.getInt("coach_id"), rs.getInt("train_id"),
            rs.getString("coach_name"), rs.getString("coach_type"),
            rs.getInt("total_seats"), rs.getDouble("fare_per_km")
        );
    }
}
