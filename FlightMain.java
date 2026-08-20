package flight;

import common.exceptions.*;
import common.utilities.AuthManager;
import common.utilities.ConsoleUtils;
import common.validation.Validator;
import datastructures.linkedlist.CustomLinkedList;
import flight.dao.FlightDAO;
import flight.models.Flight;
import flight.models.FlightPassenger;
import flight.models.LuggageInfo;
import flight.services.FlightAdminService;
import flight.services.FlightBookingService;
import flight.tickets.BoardingPassGenerator;

/**
 * FlightMain — Console UI entry point for the EasyGo Flight system.
 *
 * User  menu : Search | Book | View Booking | History | Cancel | Boarding Pass
 * Admin menu : Add Flight | Update Status | All Bookings | Occupancy | Seed
 */
public class FlightMain {

    private final FlightBookingService bookingService;
    private final FlightAdminService   adminService;
    private final BoardingPassGenerator boardingPass;
    private final FlightDAO            flightDAO;

    public FlightMain() {
        this.bookingService = new FlightBookingService();
        this.adminService   = new FlightAdminService();
        this.boardingPass   = new BoardingPassGenerator();
        this.flightDAO      = new FlightDAO();
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
                "Search Flights",
                "Book Flight",
                "View Booking Details",
                "My Bookings",
                "Cancel Booking",
                "Print Boarding Pass",
                "Cancellation Policy"
            };
            int choice = ConsoleUtils.showMenu("EASYGO FLIGHT SYSTEM", opts);
            if (choice == 0) break;

            try {
                switch (choice) {
                    case 1 -> searchFlow();
                    case 2 -> bookFlow();
                    case 3 -> {
                        String ref = ConsoleUtils.readStringNonEmpty("Booking Reference");
                        bookingService.showBookingDetails(ref);
                    }
                    case 4 -> bookingService.showHistory(AuthManager.getUserId());
                    case 5 -> cancelFlow();
                    case 6 -> {
                        String ref = ConsoleUtils.readStringNonEmpty("Booking Reference");
                        boardingPass.reprint(ref);
                    }
                    case 7 -> new common.payment.PaymentService()
                                  .showCancellationPolicy("FLIGHT");
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
                "Add New Flight",
                "Update Flight Status",
                "View All Bookings",
                "Occupancy Report",
                "Seed Sample Data",
                "Search Flights (Test)"
            };
            int choice = ConsoleUtils.showMenu("FLIGHT — ADMIN PANEL", opts);
            if (choice == 0) break;

            try {
                switch (choice) {
                    case 1 -> adminService.addFlightInteractive();
                    case 2 -> adminService.updateFlightStatus();
                    case 3 -> adminService.viewAllBookings();
                    case 4 -> adminService.occupancyReport();
                    case 5 -> adminService.seedSampleData();
                    case 6 -> searchFlow();
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
        ConsoleUtils.printHeader("SEARCH FLIGHTS");
        String origin = ConsoleUtils.readStringNonEmpty("Origin (City / IATA)");
        String dest   = ConsoleUtils.readStringNonEmpty("Destination (City / IATA)");
        String date   = ConsoleUtils.readStringNonEmpty("Journey Date (YYYY-MM-DD)");
        Validator.validDateFormat(date);

        CustomLinkedList<Flight> flights = bookingService.searchFlights(origin, dest);
        bookingService.displayFlights(flights, date);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: BOOK
    // ══════════════════════════════════════════════════════════════════════════

    private void bookFlow() throws ReservationException {
        ConsoleUtils.printHeader("BOOK FLIGHT TICKET");

        // Step 1: Search
        String origin = ConsoleUtils.readStringNonEmpty("Origin");
        String dest   = ConsoleUtils.readStringNonEmpty("Destination");
        String date   = ConsoleUtils.readStringNonEmpty("Journey Date (YYYY-MM-DD)");
        Validator.futureDate(date);

        CustomLinkedList<Flight> flights = bookingService.searchFlights(origin, dest);
        bookingService.displayFlights(flights, date);

        // Step 2: Select flight
        int fIdx = ConsoleUtils.readIntInRange("Select Flight #", 1, flights.size()) - 1;
        Flight selected = flights.get(fIdx);

        // Step 3: Select class
        String[] classes = {"ECONOMY", "BUSINESS", "FIRST"};
        int cIdx = ConsoleUtils.showMenu("SELECT CLASS", classes);
        if (cIdx == 0) return;
        String classType = classes[cIdx - 1];

        // Check availability
        int avail = bookingService.availableSeats(selected.getFlightId(), classType, date);
        if (avail == 0) {
            ConsoleUtils.printError("No " + classType + " seats available on this flight.");
            return;
        }
        ConsoleUtils.printInfo("Available " + classType + " seats: " + avail);

        // Step 4: Passengers
        int pCount = ConsoleUtils.readIntInRange("Number of Passengers", 1, 6);

        // Show fare preview before collecting details
        bookingService.getPricingService().printFareBreakdown(
            selected.getFlightId(), selected.getDistanceKm(),
            classType, date, avail, pCount);

        if (!ConsoleUtils.readYesNo("Proceed with booking")) return;

        // Step 5: Collect passenger details
        CustomLinkedList<FlightPassenger> passengers    = new CustomLinkedList<>();
        CustomLinkedList<String>          mealChoices   = new CustomLinkedList<>();
        CustomLinkedList<Double>          luggageWeights = new CustomLinkedList<>();

        for (int i = 0; i < pCount; i++) {
            ConsoleUtils.printSubHeader("PASSENGER " + (i+1) + " DETAILS");
            FlightPassenger p = collectPassengerDetails();
            passengers.add(p);

            // Meal selection
            String meal = selectMeal(classType);
            mealChoices.add(meal);

            // Luggage
            double freeKg = switch (classType) {
                case "BUSINESS" -> 25.0;
                case "FIRST"    -> 35.0;
                default         -> 15.0;
            };
            ConsoleUtils.printInfo("Free luggage allowance: " + freeKg + " kg");
            double luggage = ConsoleUtils.readDouble("Luggage weight (kg)");
            luggageWeights.add(luggage);

            double extra = LuggageInfo.calculateExtra(luggage, classType);
            if (extra > 0)
                ConsoleUtils.printInfo(String.format(
                    "Extra luggage charge: Rs. %.2f", extra));
        }

        // Step 6: Book
        String ref = bookingService.bookFlight(
            AuthManager.getUserId(),
            selected.getFlightId(),
            classType, date,
            passengers, mealChoices, luggageWeights);

        // Step 7: Print boarding pass
        boardingPass.printBoardingPass(ref);
        if (ConsoleUtils.readYesNo("Save boarding pass to file"))
            boardingPass.saveBoardingPass(ref);
    }

    // ── Collect single passenger ──────────────────────────────────────────────
    private FlightPassenger collectPassengerDetails() throws ValidationException {
        String name   = ConsoleUtils.readStringNonEmpty("Full Name");
        Validator.validName(name, "name");
        int    age    = ConsoleUtils.readIntInRange("Age", 1, 120);
        String gender = ConsoleUtils.readStringNonEmpty("Gender (M/F/O)").toUpperCase();
        Validator.validGender(gender);
        String passport = ConsoleUtils.readStringNonEmpty("Passport / ID Number");
        String nation   = ConsoleUtils.readStringNonEmpty("Nationality");

        FlightPassenger p = new FlightPassenger();
        p.setName       (name);
        p.setAge        (age);
        p.setGender     (gender);
        p.setPassportNo (passport);
        p.setNationality(nation);
        return p;
    }

    // ── Meal selection ────────────────────────────────────────────────────────
    private String selectMeal(String classType) {
        String[] meals = {"VEG", "NON_VEG", "VEGAN", "JAIN", "DIABETIC", "NONE"};
        ConsoleUtils.printSubHeader("MEAL SELECTION");
        if (!"ECONOMY".equalsIgnoreCase(classType))
            ConsoleUtils.printInfo("Meal is complimentary for " + classType + " class.");
        else
            ConsoleUtils.printInfo("Meal charge: Rs. 200 (Economy class)");

        int choice = ConsoleUtils.showMenu("CHOOSE MEAL", meals);
        return (choice == 0) ? "VEG" : meals[choice - 1];
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: CANCEL
    // ══════════════════════════════════════════════════════════════════════════

    private void cancelFlow() throws ReservationException {
        ConsoleUtils.printHeader("CANCEL FLIGHT BOOKING");
        bookingService.showHistory(AuthManager.getUserId());
        String ref = ConsoleUtils.readStringNonEmpty("Enter Booking Reference");
        if (!ConsoleUtils.readYesNo("Confirm cancellation of " + ref)) {
            ConsoleUtils.printInfo("Cancellation aborted.");
            return;
        }
        bookingService.cancelBooking(ref);
    }
}