package bus;

import common.exceptions.*;
import common.utilities.AuthManager;
import common.utilities.ConsoleUtils;
import common.validation.Validator;
import datastructures.linkedlist.CustomLinkedList;
import bus.dao.BusDAO;
import bus.models.Bus;
import bus.models.BusPassenger;
import bus.services.BusAdminService;
import bus.services.BusBookingService;
import bus.services.BusSeatManager;
import bus.tickets.BusTicketGenerator;

/**
 * BusMain — Console UI entry point for the EasyGo Bus system.
 *
 * User  menu : Search | Book | View Booking | My History | Cancel |
 *              Seat Layout | Reprint | Policy
 * Admin menu : Add Bus | Update Status | All Bookings |
 *              Occupancy | Route Network | Seed
 */
public class BusMain {

    private final BusBookingService bookingService;
    private final BusAdminService   adminService;
    private final BusTicketGenerator ticketGen;
    private final BusSeatManager    seatManager;
    private final BusDAO            busDAO;

    public BusMain() {
        this.bookingService = new BusBookingService();
        this.adminService   = new BusAdminService();
        this.ticketGen      = new BusTicketGenerator();
        this.seatManager    = new BusSeatManager();
        this.busDAO         = new BusDAO();
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public void start() {
        if (AuthManager.isAdmin()) adminMenu();
        else                       userMenu();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  USER MENU
    // ══════════════════════════════════════════════════════════════════════════

    private void userMenu() {
        while (true) {
            String[] opts = {
                "Search Buses",
                "Book Bus Ticket",
                "View Booking Details",
                "My Bookings",
                "Cancel Booking",
                "View Seat Layout",
                "Reprint Ticket",
                "Cancellation Policy"
            };
            int choice = ConsoleUtils.showMenu("EASYGO BUS SYSTEM", opts);
            if (choice == 0) break;

            try {
                switch (choice) {
                    case 1 -> searchFlow();
                    case 2 -> bookFlow();
                    case 3 -> {
                        String ref = ConsoleUtils.readStringNonEmpty("Booking Reference");
                        bookingService.showDetails(ref);
                    }
                    case 4 -> bookingService.showHistory(AuthManager.getUserId());
                    case 5 -> cancelFlow();
                    case 6 -> seatLayoutFlow();
                    case 7 -> {
                        String ref = ConsoleUtils.readStringNonEmpty("Booking Reference");
                        ticketGen.reprintTicket(ref);
                    }
                    case 8 -> new common.payment.PaymentService()
                                  .showCancellationPolicy("BUS");
                }
            } catch (ReservationException e) {
                ConsoleUtils.printError(e.toString());
            } catch (Exception e) {
                ConsoleUtils.printError("Unexpected error: " + e.getMessage());
            }
            ConsoleUtils.pause();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ADMIN MENU
    // ══════════════════════════════════════════════════════════════════════════

    private void adminMenu() {
        while (true) {
            String[] opts = {
                "Add New Bus",
                "Update Bus Status",
                "View All Bookings",
                "Occupancy Report",
                "View Route Network",
                "Shortest Path Fare Calculator",
                "Seed Sample Data",
                "Search Buses (Test)"
            };
            int choice = ConsoleUtils.showMenu("BUS — ADMIN PANEL", opts);
            if (choice == 0) break;

            try {
                switch (choice) {
                    case 1 -> adminService.addBusInteractive();
                    case 2 -> adminService.updateBusStatus();
                    case 3 -> adminService.viewAllBookings();
                    case 4 -> adminService.occupancyReport();
                    case 5 -> bookingService.getFareCalc().printRouteNetwork();
                    case 6 -> shortestPathFlow();
                    case 7 -> adminService.seedSampleData();
                    case 8 -> searchFlow();
                }
            } catch (Exception e) {
                ConsoleUtils.printError("Admin error: " + e.getMessage());
            }
            ConsoleUtils.pause();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: SEARCH
    // ══════════════════════════════════════════════════════════════════════════

    private void searchFlow() throws ReservationException {
        ConsoleUtils.printHeader("SEARCH BUSES");
        String origin = ConsoleUtils.readStringNonEmpty("Origin City");
        String dest   = ConsoleUtils.readStringNonEmpty("Destination City");
        String date   = ConsoleUtils.readStringNonEmpty("Journey Date (YYYY-MM-DD)");
        Validator.validDateFormat(date);

        CustomLinkedList<Bus> buses = bookingService.searchBuses(origin, dest);
        bookingService.displayBuses(buses, date);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: BOOK
    // ══════════════════════════════════════════════════════════════════════════

    private void bookFlow() throws ReservationException {
        ConsoleUtils.printHeader("BOOK BUS TICKET");

        // Step 1 — Route & date
        String origin = ConsoleUtils.readStringNonEmpty("Origin City");
        String dest   = ConsoleUtils.readStringNonEmpty("Destination City");
        String date   = ConsoleUtils.readStringNonEmpty("Journey Date (YYYY-MM-DD)");
        Validator.futureDate(date);

        // Step 2 — Display buses
        CustomLinkedList<Bus> buses = bookingService.searchBuses(origin, dest);
        bookingService.displayBuses(buses, date);

        // Step 3 — Select bus
        int bIdx = ConsoleUtils.readIntInRange("Select Bus #", 1, buses.size()) - 1;
        Bus selected = buses.get(bIdx);

        // Check availability
        int avail = seatManager.getAvailableCount(
                selected.getBusId(), date, selected.getTotalSeats());
        if (avail == 0) {
            ConsoleUtils.printError("No seats available on this bus for " + date);
            return;
        }
        ConsoleUtils.printInfo("Available seats: " + avail);
        ConsoleUtils.printInfo("Bus Type       : " + selected.getBusType());

        // Step 4 — Passenger count
        int pCount = ConsoleUtils.readIntInRange("Number of Passengers", 1, 6);

        // Step 5 — Fare preview
        bookingService.getFareCalc().printFareBreakdown(
            selected.getDistanceKm(), selected.getFarePerKm(),
            selected.getBusType(), pCount);

        if (!ConsoleUtils.readYesNo("Proceed with booking")) return;

        // Step 6 — Seat preference
        boolean preferLower = false;
        if (selected.getBusType().toUpperCase().contains("SLEEPER")) {
            preferLower = ConsoleUtils.readYesNo("Prefer lower berth");
        }

        // Step 7 — Collect passengers
        CustomLinkedList<BusPassenger> passengers = new CustomLinkedList<>();
        for (int i = 0; i < pCount; i++) {
            ConsoleUtils.printSubHeader("PASSENGER " + (i + 1) + " DETAILS");
            passengers.add(collectPassengerDetails());
        }

        // Step 8 — Book
        String ref = bookingService.bookBus(
            AuthManager.getUserId(),
            selected.getBusId(),
            date, passengers, preferLower);

        // Step 9 — Print ticket
        ticketGen.printTicket(ref);
        if (ConsoleUtils.readYesNo("Save ticket to file"))
            ticketGen.writeTicketFile(ref);
    }

    // ── Collect one passenger's details ──────────────────────────────────────
    private BusPassenger collectPassengerDetails() throws ValidationException {
        String name   = ConsoleUtils.readStringNonEmpty("Full Name");
        Validator.validName(name, "name");

        int    age    = ConsoleUtils.readIntInRange("Age", 1, 120);
        Validator.validAge(age);

        String gender = ConsoleUtils.readStringNonEmpty("Gender (M/F/O)").toUpperCase();
        Validator.validGender(gender);

        String idType = ConsoleUtils.readStringNonEmpty("ID Type (AADHAR/PAN/DL)");
        String idNum  = ConsoleUtils.readStringNonEmpty("ID Number");

        BusPassenger p = new BusPassenger();
        p.setName    (name);
        p.setAge     (age);
        p.setGender  (gender);
        p.setIdType  (idType);
        p.setIdNumber(idNum);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: CANCEL
    // ══════════════════════════════════════════════════════════════════════════

    private void cancelFlow() throws ReservationException {
        ConsoleUtils.printHeader("CANCEL BUS BOOKING");
        bookingService.showHistory(AuthManager.getUserId());

        String ref = ConsoleUtils.readStringNonEmpty("Enter Booking Reference to cancel");
        ticketGen.printSummaryCard(ref);

        if (!ConsoleUtils.readYesNo("Confirm cancellation")) {
            ConsoleUtils.printInfo("Cancellation aborted.");
            return;
        }
        bookingService.cancelBooking(ref);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: SEAT LAYOUT
    // ══════════════════════════════════════════════════════════════════════════

    private void seatLayoutFlow() throws DatabaseException {
        ConsoleUtils.printHeader("VIEW SEAT LAYOUT");
        String busNum = ConsoleUtils.readStringNonEmpty("Bus Number");
        String date   = ConsoleUtils.readStringNonEmpty("Journey Date (YYYY-MM-DD)");

        Bus bus = busDAO.getByNumber(busNum);
        if (bus == null) { ConsoleUtils.printError("Bus not found: " + busNum); return; }

        int avail = seatManager.getAvailableCount(bus.getBusId(), date, bus.getTotalSeats());
        ConsoleUtils.printInfo("Total: " + bus.getTotalSeats()
            + "  |  Available: " + avail
            + "  |  Booked: " + (bus.getTotalSeats() - avail));

        seatManager.printLayout(bus.getBusId(), bus.getBusType(), date);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: SHORTEST PATH FARE (admin)
    // ══════════════════════════════════════════════════════════════════════════

    private void shortestPathFlow() {
        ConsoleUtils.printHeader("SHORTEST PATH FARE CALCULATOR");
        String origin  = ConsoleUtils.readStringNonEmpty("Origin City");
        String dest    = ConsoleUtils.readStringNonEmpty("Destination City");
        double fareKm  = ConsoleUtils.readDouble("Fare per km (Rs.)");
        String[] types = {"SEATER","SLEEPER","AC_SEATER","AC_SLEEPER"};
        int tc         = ConsoleUtils.showMenu("Bus Type", types);
        if (tc == 0) return;
        String busType = types[tc - 1];

        bookingService.getFareCalc().printShortestPath(origin, dest);

        double fare = bookingService.getFareCalc()
                          .shortestPathFare(origin, dest, fareKm, busType);
        if (fare < 0) {
            ConsoleUtils.printError("No route found between " + origin + " and " + dest);
        } else {
            ConsoleUtils.printInfo(String.format(
                "Estimated fare (%s): Rs. %.2f", busType, fare));
        }
    }
}