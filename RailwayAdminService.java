package railway.services;

import common.database.DBConnection;
import common.exceptions.DatabaseException;
import common.utilities.ConsoleUtils;
import datastructures.linkedlist.CustomLinkedList;
import railway.dao.BookingDAO;
import railway.dao.TrainDAO;
import railway.dao.WaitingListDAO;
import railway.management.SeatManager;
import railway.management.WaitingListManager;
import railway.models.Booking;
import railway.models.Coach;
import railway.models.Train;

/**
 * RailwayAdminService — admin-only operations.
 *
 * Features:
 *   - Add / update / deactivate trains and coaches
 *   - Add routes
 *   - View all bookings
 *   - View WL / RAC status per train/coach/date
 *   - Seat occupancy report
 *   - Seed sample data
 */
public class RailwayAdminService {

    private final TrainDAO          trainDAO;
    private final BookingDAO        bookingDAO;
    private final WaitingListDAO    wlDAO;
    private final SeatManager       seatManager;
    private final WaitingListManager wlManager;

    public RailwayAdminService() {
        this.trainDAO   = new TrainDAO();
        this.bookingDAO = new BookingDAO();
        this.wlDAO      = new WaitingListDAO();
        this.seatManager = new SeatManager();
        this.wlManager  = new WaitingListManager();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ADD TRAIN
    // ══════════════════════════════════════════════════════════════════════════

    public void addTrainInteractive() throws DatabaseException {
        ConsoleUtils.printHeader("ADD NEW TRAIN");

        String number = ConsoleUtils.readStringNonEmpty("Train Number (e.g. 12301)");
        String name   = ConsoleUtils.readStringNonEmpty("Train Name");
        String origin = ConsoleUtils.readStringNonEmpty("Origin Station");
        String dest   = ConsoleUtils.readStringNonEmpty("Destination Station");
        double dist   = ConsoleUtils.readDouble("Distance (km)");
        String dep    = ConsoleUtils.readStringNonEmpty("Departure Time (HH:MM)");
        String arr    = ConsoleUtils.readStringNonEmpty("Arrival Time (HH:MM)");
        int    seats  = ConsoleUtils.readInt("Total Seats");

        DBConnection db = DBConnection.getInstance();
        try {
            db.beginTransaction();
            int routeId = trainDAO.insertRoute(origin, dest, dist);
            int trainId = trainDAO.insertTrain(number, name, routeId, dep, arr, seats);

            // Add coaches interactively
            boolean addMore = true;
            while (addMore) {
                ConsoleUtils.printSubHeader("ADD COACH");
                String coachName = ConsoleUtils.readStringNonEmpty("Coach Name (e.g. S1)");
                String[] types = {"SL","3A","2A","1A","CC","GEN"};
                int typeChoice = ConsoleUtils.showMenu("Coach Type", types);
                if (typeChoice == 0) break;
                String coachType  = types[typeChoice - 1];
                int    coachSeats = ConsoleUtils.readInt("Seats in this coach");
                double farePerKm  = ConsoleUtils.readDouble("Fare per km (Rs.)");
                trainDAO.insertCoach(trainId, coachName, coachType, coachSeats, farePerKm);
                addMore = ConsoleUtils.readYesNo("Add another coach");
            }
            db.commit();
            ConsoleUtils.printSuccess("Train " + number + " — " + name + " added successfully.");
        } catch (Exception e) {
            db.rollback();
            ConsoleUtils.printError("Failed to add train: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE TRAIN STATUS
    // ══════════════════════════════════════════════════════════════════════════

    public void updateTrainStatus() throws DatabaseException {
        String number = ConsoleUtils.readStringNonEmpty("Enter Train Number");
        Train t = trainDAO.getTrainByNumber(number);
        if (t == null) { ConsoleUtils.printError("Train not found: " + number); return; }

        ConsoleUtils.printInfo("Current Status: " + t.getStatus());
        String[] statuses = {"ACTIVE", "CANCELLED", "MAINTENANCE"};
        int choice = ConsoleUtils.showMenu("New Status", statuses);
        if (choice == 0) return;

        trainDAO.updateTrainStatus(t.getTrainId(), statuses[choice - 1]);
        ConsoleUtils.printSuccess("Status updated to: " + statuses[choice - 1]);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VIEW ALL BOOKINGS
    // ══════════════════════════════════════════════════════════════════════════

    public void viewAllBookings() throws DatabaseException {
        // Admin view: fetch all bookings across users
        // Using a query via BookingDAO (need to add getAllBookings)
        ConsoleUtils.printHeader("ALL RAILWAY BOOKINGS");
        ConsoleUtils.printInfo("Filter options:");
        String[] filters = {"All Bookings", "Confirmed Only", "WL Only",
                             "RAC Only", "Cancelled Only"};
        int choice = ConsoleUtils.showMenu("FILTER", filters);
        if (choice == 0) return;

        String[] statusMap = {null, "CONFIRMED", "WAITING", "RAC", "CANCELLED"};
        String filter = statusMap[choice - 1];

        CustomLinkedList<Booking> bookings = bookingDAO.getAllBookings(filter);
        if (bookings.isEmpty()) {
            ConsoleUtils.printInfo("No bookings found.");
            return;
        }

        String[] headers = {"PNR", "Train", "From→To", "Date", "Status", "Fare", "User"};
        int[]    widths  = {16, 20, 22, 12, 10, 10, 8};
        ConsoleUtils.printTableHeader(headers, widths);
        for (int i = 0; i < bookings.size(); i++) {
            Booking b = bookings.get(i);
            ConsoleUtils.printTableRow(new String[]{
                b.getPnr(), b.getTrainName(),
                b.getOrigin() + "→" + b.getDestination(),
                b.getJourneyDate(), b.getStatus(),
                String.format("%.2f", b.getTotalFare()),
                String.valueOf(b.getUserId())
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);
        ConsoleUtils.printInfo("Total: " + bookings.size() + " bookings.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEAT OCCUPANCY REPORT
    // ══════════════════════════════════════════════════════════════════════════

    public void seatOccupancyReport() throws DatabaseException {
        String date = ConsoleUtils.readStringNonEmpty("Enter Date (YYYY-MM-DD)");
        ConsoleUtils.printHeader("SEAT OCCUPANCY REPORT — " + date);

        CustomLinkedList<Train> trains = trainDAO.getAllTrains();
        String[] headers = {"Train", "Coach", "Type", "Total", "Booked", "Avail", "RAC", "WL"};
        int[]    widths  = {20, 7, 5, 6, 7, 6, 5, 5};
        ConsoleUtils.printTableHeader(headers, widths);

        for (int i = 0; i < trains.size(); i++) {
            Train t = trains.get(i);
            CustomLinkedList<Coach> coaches = trainDAO.getCoachesByTrain(t.getTrainId());
            for (int j = 0; j < coaches.size(); j++) {
                Coach c = coaches.get(j);
                int avail   = seatManager.getAvailableSeats(t.getTrainId(), c.getCoachId(), date);
                int booked  = c.getTotalSeats() - avail;
                int racCnt  = wlDAO.getRACCount(t.getTrainId(), c.getCoachId(), date);
                int wlCnt   = wlDAO.getWLCount(t.getTrainId(), c.getCoachId(), date);
                ConsoleUtils.printTableRow(new String[]{
                    t.getTrainName(), c.getCoachName(), c.getCoachType(),
                    String.valueOf(c.getTotalSeats()), String.valueOf(booked),
                    String.valueOf(avail), String.valueOf(racCnt), String.valueOf(wlCnt)
                }, widths);
            }
        }
        ConsoleUtils.printTableSeparator(widths);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEED SAMPLE DATA
    // ══════════════════════════════════════════════════════════════════════════

    public void seedSampleData() throws DatabaseException {
        ConsoleUtils.printInfo("Seeding sample railway data...");
        DBConnection db = DBConnection.getInstance();
        try {
            db.beginTransaction();

            // Routes
            int r1 = trainDAO.insertRoute("MUMBAI", "DELHI",      1400);
            int r2 = trainDAO.insertRoute("DELHI",  "KOLKATA",    1500);
            int r3 = trainDAO.insertRoute("CHENNAI","BANGALORE",   350);
            int r4 = trainDAO.insertRoute("MUMBAI", "AHMEDABAD",   530);
            int r5 = trainDAO.insertRoute("DELHI",  "JAIPUR",      300);

            // Trains with coaches
            int t1 = trainDAO.insertTrain("12301","RAJDHANI EXPRESS",  r1,"16:00","08:00",500);
            trainDAO.insertCoach(t1,"S1","SL",72,0.45); trainDAO.insertCoach(t1,"B1","3A",64,0.85);
            trainDAO.insertCoach(t1,"A1","2A",46,1.20); trainDAO.insertCoach(t1,"H1","1A",24,1.80);

            int t2 = trainDAO.insertTrain("12302","DURONTO EXPRESS",   r2,"07:30","19:45",480);
            trainDAO.insertCoach(t2,"S1","SL",72,0.45); trainDAO.insertCoach(t2,"S2","SL",72,0.45);
            trainDAO.insertCoach(t2,"B1","3A",64,0.85); trainDAO.insertCoach(t2,"A1","2A",46,1.20);

            int t3 = trainDAO.insertTrain("12027","SHATABDI EXPRESS",  r3,"06:00","11:30",350);
            trainDAO.insertCoach(t3,"C1","CC",78,0.90); trainDAO.insertCoach(t3,"C2","CC",78,0.90);
            trainDAO.insertCoach(t3,"E1","CC",50,1.50);

            int t4 = trainDAO.insertTrain("19011","GUJARAT MAIL",       r4,"21:40","07:10",400);
            trainDAO.insertCoach(t4,"S1","SL",72,0.40); trainDAO.insertCoach(t4,"S2","SL",72,0.40);
            trainDAO.insertCoach(t4,"B1","3A",64,0.75); trainDAO.insertCoach(t4,"G1","GEN",90,0.20);

            int t5 = trainDAO.insertTrain("12015","AJMER SHATABDI",     r5,"06:05","10:40",280);
            trainDAO.insertCoach(t5,"C1","CC",78,0.80); trainDAO.insertCoach(t5,"C2","CC",78,0.80);

            db.commit();
            ConsoleUtils.printSuccess("Sample data seeded: 5 trains, 5 routes, 15 coaches.");
        } catch (Exception e) {
            db.rollback();
            ConsoleUtils.printError("Seed failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  WL / RAC MONITOR
    // ══════════════════════════════════════════════════════════════════════════

    public void monitorWLRAC() throws DatabaseException {
        String date     = ConsoleUtils.readStringNonEmpty("Date (YYYY-MM-DD)");
        String trainNum = ConsoleUtils.readStringNonEmpty("Train Number");
        Train train = trainDAO.getTrainByNumber(trainNum);
        if (train == null) { ConsoleUtils.printError("Train not found."); return; }

        CustomLinkedList<Coach> coaches = trainDAO.getCoachesByTrain(train.getTrainId());
        for (int i = 0; i < coaches.size(); i++) {
            Coach c = coaches.get(i);
            wlManager.displayWLStatus(train.getTrainId(), c.getCoachId(), date);
        }
    }
}