package flight.services;

import common.database.DBConnection;
import common.exceptions.DatabaseException;
import common.utilities.ConsoleUtils;
import datastructures.linkedlist.CustomLinkedList;
import flight.dao.FlightBookingDAO;
import flight.dao.FlightDAO;
import flight.models.Flight;
import flight.models.FlightBooking;

/**
 * FlightAdminService — admin operations for the Flight system.
 *
 * Features:
 *   - Add flights + auto-generate seats
 *   - Update flight status
 *   - View all bookings with filter
 *   - Seat occupancy report
 *   - Seed sample data
 */
public class FlightAdminService {

    private final FlightDAO        flightDAO;
    private final FlightBookingDAO bookingDAO;
    private final FlightBookingService bookingService;

    public FlightAdminService() {
        this.flightDAO      = new FlightDAO();
        this.bookingDAO     = new FlightBookingDAO();
        this.bookingService = new FlightBookingService();
    }

    // ── Add flight ────────────────────────────────────────────────────────────
    public void addFlightInteractive() throws DatabaseException {
        ConsoleUtils.printHeader("ADD NEW FLIGHT");

        String number  = ConsoleUtils.readStringNonEmpty("Flight Number (e.g. AI-101)");
        String airline = ConsoleUtils.readStringNonEmpty("Airline Name");
        String origin  = ConsoleUtils.readStringNonEmpty("Origin (IATA / City)");
        String dest    = ConsoleUtils.readStringNonEmpty("Destination (IATA / City)");
        double dist    = ConsoleUtils.readDouble("Distance (km)");
        String dep     = ConsoleUtils.readStringNonEmpty("Departure Time (HH:MM)");
        String arr     = ConsoleUtils.readStringNonEmpty("Arrival Time (HH:MM)");
        int eSeats     = ConsoleUtils.readInt("Economy Seats");
        int bSeats     = ConsoleUtils.readInt("Business Seats");
        int fSeats     = ConsoleUtils.readInt("First Class Seats");

        DBConnection db = DBConnection.getInstance();
        try {
            db.beginTransaction();
            int routeId  = flightDAO.insertRoute(origin, dest, dist);
            int flightId = flightDAO.insertFlight(number, airline, routeId, dep, arr,
                                                   eSeats + bSeats + fSeats);
            flightDAO.generateSeats(flightId, eSeats, bSeats, fSeats);
            db.commit();
            ConsoleUtils.printSuccess("Flight " + number + " added with "
                + (eSeats + bSeats + fSeats) + " seats.");
        } catch (Exception e) {
            db.rollback();
            ConsoleUtils.printError("Failed to add flight: " + e.getMessage());
        }
    }

    // ── Update flight status ──────────────────────────────────────────────────
    public void updateFlightStatus() throws DatabaseException {
        String number = ConsoleUtils.readStringNonEmpty("Flight Number");
        Flight f = flightDAO.getByNumber(number);
        if (f == null) { ConsoleUtils.printError("Flight not found."); return; }
        ConsoleUtils.printInfo("Current status: " + f.getStatus());
        String[] opts = {"SCHEDULED","DELAYED","CANCELLED","DEPARTED"};
        int choice = ConsoleUtils.showMenu("New Status", opts);
        if (choice == 0) return;
        flightDAO.updateStatus(f.getFlightId(), opts[choice - 1]);
        ConsoleUtils.printSuccess("Status updated to: " + opts[choice - 1]);
    }

    // ── View all bookings ─────────────────────────────────────────────────────
    public void viewAllBookings() throws DatabaseException {
        String[] filters = {"All","CONFIRMED","CANCELLED","PENDING"};
        int c = ConsoleUtils.showMenu("FILTER BOOKINGS", filters);
        if (c == 0) return;
        String filter = c == 1 ? null : filters[c - 1];

        CustomLinkedList<FlightBooking> list = bookingDAO.getAllBookings(filter);
        ConsoleUtils.printHeader("ALL FLIGHT BOOKINGS");
        if (list.isEmpty()) { ConsoleUtils.printInfo("No bookings."); return; }

        String[] h = {"Ref","Flight","From→To","Date","Class","Status","Fare"};
        int[]    w = {14,8,18,12,9,10,10};
        ConsoleUtils.printTableHeader(h, w);
        for (int i = 0; i < list.size(); i++) {
            FlightBooking b = list.get(i);
            ConsoleUtils.printTableRow(new String[]{
                b.getBookingRef(), b.getFlightNumber(),
                b.getOrigin()+"→"+b.getDestination(),
                b.getJourneyDate(), b.getClassType(),
                b.getStatus(), String.format("%.2f", b.getTotalFare())
            }, w);
        }
        ConsoleUtils.printTableSeparator(w);
        ConsoleUtils.printInfo("Total: " + list.size());
    }

    // ── Occupancy report ──────────────────────────────────────────────────────
    public void occupancyReport() throws DatabaseException {
        String date = ConsoleUtils.readStringNonEmpty("Date (YYYY-MM-DD)");
        ConsoleUtils.printHeader("FLIGHT OCCUPANCY REPORT — " + date);

        CustomLinkedList<Flight> flights = flightDAO.getAllFlights();
        String[] h = {"Flight","Airline","Route","Eco Avail","Biz Avail","1st Avail"};
        int[]    w = {8,14,20,10,10,10};
        ConsoleUtils.printTableHeader(h, w);

        for (int i = 0; i < flights.size(); i++) {
            Flight f = flights.get(i);
            int eA = bookingService.availableSeats(f.getFlightId(), "ECONOMY",  date);
            int bA = bookingService.availableSeats(f.getFlightId(), "BUSINESS", date);
            int fA = bookingService.availableSeats(f.getFlightId(), "FIRST",    date);
            ConsoleUtils.printTableRow(new String[]{
                f.getFlightNumber(), f.getAirline(),
                f.getOrigin()+"→"+f.getDestination(),
                String.valueOf(eA), String.valueOf(bA), String.valueOf(fA)
            }, w);
        }
        ConsoleUtils.printTableSeparator(w);
    }

    // ── Seed sample data ──────────────────────────────────────────────────────
    public void seedSampleData() throws DatabaseException {
        ConsoleUtils.printInfo("Seeding sample flight data...");
        DBConnection db = DBConnection.getInstance();
        try {
            db.beginTransaction();

            int r1 = flightDAO.insertRoute("DEL","BOM",1150);
            int r2 = flightDAO.insertRoute("BOM","DEL",1150);
            int r3 = flightDAO.insertRoute("DEL","BLR",1740);
            int r4 = flightDAO.insertRoute("BOM","BLR", 840);
            int r5 = flightDAO.insertRoute("DEL","HYD",1250);
            int r6 = flightDAO.insertRoute("BOM","CCU",1660);

            int f1 = flightDAO.insertFlight("AI-101","Air India",   r1,"06:00","08:10",186);
            flightDAO.generateSeats(f1, 150, 28, 8);

            int f2 = flightDAO.insertFlight("6E-201","IndiGo",      r2,"07:30","09:45",180);
            flightDAO.generateSeats(f2, 160, 16, 4);

            int f3 = flightDAO.insertFlight("AI-505","Air India",   r3,"09:00","12:30",210);
            flightDAO.generateSeats(f3, 170, 30, 10);

            int f4 = flightDAO.insertFlight("SG-301","SpiceJet",    r4,"11:00","12:50",168);
            flightDAO.generateSeats(f4, 148, 16, 4);

            int f5 = flightDAO.insertFlight("UK-201","Vistara",     r5,"14:00","16:15",200);
            flightDAO.generateSeats(f5, 160, 28, 12);

            int f6 = flightDAO.insertFlight("6E-888","IndiGo",      r6,"05:30","08:00",174);
            flightDAO.generateSeats(f6, 154, 16, 4);

            db.commit();
            ConsoleUtils.printSuccess("Seeded 6 flights with seats across 6 routes.");
        } catch (Exception e) {
            db.rollback();
            ConsoleUtils.printError("Seed failed: " + e.getMessage());
        }
    }
}