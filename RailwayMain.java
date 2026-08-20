package railway;

import common.exceptions.*;
import common.utilities.AuthManager;
import common.utilities.ConsoleUtils;
import common.validation.Validator;
import datastructures.linkedlist.CustomLinkedList;
import railway.management.SeatManager;
import railway.models.Coach;
import railway.models.Passenger;
import railway.models.Train;
import railway.services.RailwayAdminService;
import railway.services.RailwayBookingService;
import railway.services.TrainSearchService;
import railway.tickets.RailwayTicketGenerator;
import railway.dao.TrainDAO;

/**
 * RailwayMain — Console UI entry point for the EasyGo Railway system.
 *
 * Menu structure:
 *   User Menu  → Search | Book | PNR Status | History | Cancel | Seat Layout
 *   Admin Menu → Add Train | Update Status | View Bookings | Occupancy | Seed
 */
public class RailwayMain {

    private final RailwayBookingService bookingService;
    private final TrainSearchService    searchService;
    private final RailwayAdminService   adminService;
    private final RailwayTicketGenerator ticketGen;
    private final TrainDAO              trainDAO;
    private final SeatManager           seatManager;

    public RailwayMain() {
        this.bookingService = new RailwayBookingService();
        this.searchService  = new TrainSearchService();
        this.adminService   = new RailwayAdminService();
        this.ticketGen      = new RailwayTicketGenerator();
        this.trainDAO       = new TrainDAO();
        this.seatManager    = new SeatManager();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ══════════════════════════════════════════════════════════════════════════

    public void start() {
        if (AuthManager.isAdmin()) {
            adminMenu();
        } else {
            userMenu();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  USER MENU
    // ══════════════════════════════════════════════════════════════════════════

    private void userMenu() {
        while (true) {
            String[] opts = {
                "Search Trains",
                "Book Ticket",
                "Check PNR Status",
                "My Bookings",
                "Cancel Ticket",
                "View Seat Layout",
                "Reprint Ticket",
                "Cancellation Policy"
            };
            int choice = ConsoleUtils.showMenu("EASYGO RAILWAY SYSTEM", opts);
            if (choice == 0) break;

            try {
                switch (choice) {
                    case 1 -> searchTrainsFlow();
                    case 2 -> bookTicketFlow();
                    case 3 -> pnrStatusFlow();
                    case 4 -> bookingService.showBookingHistory(AuthManager.getUserId());
                    case 5 -> cancelTicketFlow();
                    case 6 -> seatLayoutFlow();
                    case 7 -> reprintFlow();
                    case 8 -> showCancellationPolicy();
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
                "Add New Train",
                "Update Train Status",
                "View All Bookings",
                "Seat Occupancy Report",
                "Monitor WL / RAC",
                "Seed Sample Data",
                "Search Trains (Test)"
            };
            int choice = ConsoleUtils.showMenu("RAILWAY — ADMIN PANEL", opts);
            if (choice == 0) break;

            try {
                switch (choice) {
                    case 1 -> adminService.addTrainInteractive();
                    case 2 -> adminService.updateTrainStatus();
                    case 3 -> adminService.viewAllBookings();
                    case 4 -> adminService.seatOccupancyReport();
                    case 5 -> adminService.monitorWLRAC();
                    case 6 -> adminService.seedSampleData();
                    case 7 -> searchTrainsFlow();
                }
            } catch (Exception e) {
                ConsoleUtils.printError("Admin error: " + e.getMessage());
            }
            ConsoleUtils.pause();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: SEARCH TRAINS
    // ══════════════════════════════════════════════════════════════════════════

    private void searchTrainsFlow() throws ReservationException {
        ConsoleUtils.printHeader("SEARCH TRAINS");
        String[] searchOpts = {"By Origin & Destination", "By Train Number", "By Train Name"};
        int mode = ConsoleUtils.showMenu("SEARCH MODE", searchOpts);
        if (mode == 0) return;

        switch (mode) {
            case 1 -> {
                String origin = ConsoleUtils.readStringNonEmpty("Origin Station");
                String dest   = ConsoleUtils.readStringNonEmpty("Destination Station");
                String date   = searchService.navigateDateForSearch(origin, dest);
                if (date != null) ConsoleUtils.printInfo("Selected date: " + date);
            }
            case 2 -> {
                String num = ConsoleUtils.readStringNonEmpty("Train Number");
                Train t = searchService.searchByNumber(num);
                String date = ConsoleUtils.readStringNonEmpty("Journey Date (YYYY-MM-DD)");
                searchService.displayTrainDetails(t.getTrainId(), date);
            }
            case 3 -> {
                String kw = ConsoleUtils.readStringNonEmpty("Train Name Keyword");
                CustomLinkedList<Train> results = searchService.searchByName(kw);
                bookingService.displayTrains(results);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: BOOK TICKET
    // ══════════════════════════════════════════════════════════════════════════

    private void bookTicketFlow() throws ReservationException {
        ConsoleUtils.printHeader("BOOK RAILWAY TICKET");

        // Step 1: Route
        String origin = ConsoleUtils.readStringNonEmpty("Origin Station");
        String dest   = ConsoleUtils.readStringNonEmpty("Destination Station");

        // Step 2: Date navigation
        String date = searchService.navigateDateForSearch(origin, dest);
        if (date == null) return;

        // Step 3: Select train
        CustomLinkedList<Train> trains = bookingService.searchTrains(origin, dest);
        bookingService.displayTrains(trains);
        int tIdx = ConsoleUtils.readIntInRange("Select Train #", 1, trains.size()) - 1;
        Train selectedTrain = trains.get(tIdx);

        // Step 4: Select coach
        bookingService.displayCoachAvailability(selectedTrain.getTrainId(), date);
        CustomLinkedList<Coach> coaches = trainDAO.getCoachesByTrain(selectedTrain.getTrainId());
        if (coaches.isEmpty()) { ConsoleUtils.printError("No coaches found."); return; }
        int cIdx = ConsoleUtils.readIntInRange("Select Coach #", 1, coaches.size()) - 1;
        Coach selectedCoach = coaches.get(cIdx);

        // Step 5: Passenger count
        int pCount = ConsoleUtils.readIntInRange("Number of Passengers", 1, 6);

        // Step 6: Passenger details
        CustomLinkedList<Passenger> passengers = new CustomLinkedList<>();
        for (int i = 0; i < pCount; i++) {
            ConsoleUtils.printSubHeader("PASSENGER " + (i + 1) + " DETAILS");
            Passenger p = collectPassengerDetails();
            passengers.add(p);
        }

        // Step 7: Fare preview
        double farePerKm = selectedCoach.getFarePerKm();
        double dist      = selectedTrain.getDistanceKm();
        double fare      = Math.round(farePerKm * dist * pCount * 1.05 * 100.0) / 100.0;
        System.out.println();
        ConsoleUtils.printInfo(String.format("Estimated Fare: Rs. %.2f (incl. 5%% GST)", fare));
        System.out.println();

        if (!ConsoleUtils.readYesNo("Confirm booking")) return;

        // Step 8: Book
        String pnr = bookingService.bookTicket(
                AuthManager.getUserId(),
                selectedTrain.getTrainId(),
                selectedCoach.getCoachId(),
                date, passengers);

        // Step 9: Print ticket
        ticketGen.printTicket(pnr);

        boolean saveFile = ConsoleUtils.readYesNo("Save ticket to file");
        if (saveFile) ticketGen.writeTicketFile(pnr);
    }

    // ── Collect one passenger's details ───────────────────────────────────────
    private Passenger collectPassengerDetails() throws ValidationException {
        String name   = ConsoleUtils.readStringNonEmpty("Full Name");
        Validator.validName(name, "name");

        int age = ConsoleUtils.readIntInRange("Age", 1, 120);
        Validator.validAge(age);

        String gender = ConsoleUtils.readStringNonEmpty("Gender (M/F/O)").toUpperCase();
        Validator.validGender(gender);

        String idType = ConsoleUtils.readStringNonEmpty("ID Type (AADHAR/PAN/PASSPORT)");
        String idNum  = ConsoleUtils.readStringNonEmpty("ID Number");

        Passenger p = new Passenger();
        p.setName    (name);
        p.setAge     (age);
        p.setGender  (gender);
        p.setIdType  (idType);
        p.setIdNumber(idNum);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: PNR STATUS
    // ══════════════════════════════════════════════════════════════════════════

    private void pnrStatusFlow() throws ReservationException {
        ConsoleUtils.printHeader("CHECK PNR STATUS");
        String pnr = ConsoleUtils.readStringNonEmpty("Enter PNR Number");
        Validator.validPNR(pnr);
        bookingService.checkPNRStatus(pnr);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: CANCEL TICKET
    // ══════════════════════════════════════════════════════════════════════════

    private void cancelTicketFlow() throws ReservationException {
        ConsoleUtils.printHeader("CANCEL TICKET");

        // Show user's bookings first
        bookingService.showBookingHistory(AuthManager.getUserId());

        String pnr = ConsoleUtils.readStringNonEmpty("Enter PNR to cancel");
        Validator.validPNR(pnr);

        // Show refund preview
        ticketGen.printSummaryCard(pnr);
        if (!ConsoleUtils.readYesNo("Confirm cancellation")) {
            ConsoleUtils.printInfo("Cancellation aborted.");
            return;
        }

        bookingService.cancelByPNR(pnr);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: SEAT LAYOUT
    // ══════════════════════════════════════════════════════════════════════════

    private void seatLayoutFlow() throws DatabaseException {
        ConsoleUtils.printHeader("VIEW SEAT LAYOUT");
        String num  = ConsoleUtils.readStringNonEmpty("Train Number");
        String date = ConsoleUtils.readStringNonEmpty("Journey Date (YYYY-MM-DD)");

        Train train = trainDAO.getTrainByNumber(num);
        if (train == null) { ConsoleUtils.printError("Train not found."); return; }

        CustomLinkedList<Coach> coaches = trainDAO.getCoachesByTrain(train.getTrainId());
        if (coaches.isEmpty()) { ConsoleUtils.printInfo("No coaches."); return; }

        bookingService.displayCoachAvailability(train.getTrainId(), date);
        int cIdx = ConsoleUtils.readIntInRange("Select Coach #", 1, coaches.size()) - 1;
        seatManager.printSeatLayout(train.getTrainId(),
                coaches.get(cIdx).getCoachId(), date);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLOW: REPRINT
    // ══════════════════════════════════════════════════════════════════════════

    private void reprintFlow() throws DatabaseException {
        String pnr = ConsoleUtils.readStringNonEmpty("Enter PNR");
        ticketGen.reprintTicket(pnr);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CANCELLATION POLICY DISPLAY
    // ══════════════════════════════════════════════════════════════════════════

    private void showCancellationPolicy() {
        new common.payment.PaymentService().showCancellationPolicy("RAILWAY");
    }
}