package railway.management;

import common.exceptions.DatabaseException;
import common.utilities.ConsoleUtils;
import datastructures.hashmap.CustomHashMap;
import datastructures.linkedlist.CustomLinkedList;
import railway.dao.BookingDAO;
import railway.dao.TrainDAO;
import railway.models.Coach;

/**
 * SeatManager — manages seat layout, availability tracking,
 * and seat assignment for a coach on a given date.
 *
 * Layout per coach type:
 *   SL  / 3A / 2A / 1A : berth-based (LB/MB/UB/SL/SU per compartment)
 *   CC  (Chair Car)     : row-based   (WIN/AISLE)
 *   GEN (General)       : no reserved seat
 *
 * Uses CustomHashMap<seatNumber, status> as the availability cache.
 * RAC seats are side-lower berths (SL) — two passengers share one berth.
 */
public class SeatManager {

    // ── Constants ─────────────────────────────────────────────────────────────
    public static final int RAC_QUOTA_PER_COACH  = 4;   // RAC berths per SL coach
    public static final int MAX_WL_PER_COACH     = 10;  // max waiting list per coach

    // Berth sequence per compartment for SL/3A coach
    private static final String[] SL_BERTHS  = {"LB", "MB", "UB", "LB", "MB", "UB", "SL", "SU"};
    private static final String[] TWO_A_BERTHS = {"LB", "UB", "LB", "UB", "SL", "SU"};
    private static final String[] ONE_A_BERTHS = {"LB", "UB", "LB", "UB"};

    private final TrainDAO   trainDAO;
    private final BookingDAO bookingDAO;

    // Session cache: "trainId_coachId_date" → HashMap<seat, status>
    private final CustomHashMap<String, CustomHashMap<String, String>> cache;

    public SeatManager() {
        this.trainDAO   = new TrainDAO();
        this.bookingDAO = new BookingDAO();
        this.cache      = new CustomHashMap<>();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEAT AVAILABILITY
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns number of available (unbooked) confirmed seats.
     */
    public int getAvailableSeats(int trainId, int coachId, String date)
            throws DatabaseException {
        Coach coach = trainDAO.getCoachById(coachId);
        if (coach == null) return 0;
        int booked = bookingDAO.countBookedPassengers(trainId, coachId, date, "CONFIRMED");
        int racSeats = getRACCapacity(coach);
        int confirmCapacity = coach.getTotalSeats() - racSeats;
        return Math.max(0, confirmCapacity - booked);
    }

    /**
     * Returns true if RAC quota still has space.
     */
    public boolean isRACAvailable(int trainId, int coachId, String date)
            throws DatabaseException {
        Coach coach = trainDAO.getCoachById(coachId);
        if (coach == null) return false;
        // RAC is only for SL, 3A, 2A
        if (!isSleeperType(coach.getCoachType())) return false;
        int racBooked = bookingDAO.countBookedPassengers(trainId, coachId, date, "RAC");
        return racBooked < getRACCapacity(coach) * 2;  // 2 per RAC berth
    }

    /**
     * Returns true if waiting list still has space.
     */
    public boolean isWLAvailable(int trainId, int coachId, String date)
            throws DatabaseException {
        int wlCount = new railway.dao.WaitingListDAO().getWLCount(trainId, coachId, date);
        return wlCount < MAX_WL_PER_COACH;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEAT ASSIGNMENT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Assigns next available seat and berth for CONFIRMED booking.
     * Returns String[2] = {seatNumber, berthType} or null if full.
     */
    public String[] assignNextSeat(int trainId, int coachId, String date,
                                   String coachType, int totalSeats)
            throws DatabaseException {
        CustomLinkedList<String> booked = bookingDAO.getBookedSeats(trainId, coachId, date);
        CustomHashMap<String, Boolean> bookedMap = new CustomHashMap<>();
        for (int i = 0; i < booked.size(); i++) bookedMap.put(booked.get(i), true);

        int racSlots  = getRACCapacity(coachTypeFromString(coachType, totalSeats));
        int maxConfirm = totalSeats - racSlots;

        String[] layout = buildLayout(coachType, maxConfirm);
        for (int i = 0; i < layout.length; i += 2) {
            String seatNo = layout[i];
            String berth  = layout[i + 1];
            if (!bookedMap.containsKey(seatNo)) {
                return new String[]{seatNo, berth};
            }
        }
        return null;
    }

    /**
     * Assigns next available RAC seat (side-lower berth).
     * Returns String[2] = {seatNumber, "RAC"} or null if RAC full.
     */
    public String[] assignRACseat(int trainId, int coachId, String date,
                                  String coachType, int totalSeats)
            throws DatabaseException {
        CustomLinkedList<String> booked = bookingDAO.getBookedSeats(trainId, coachId, date);
        CustomHashMap<String, Boolean> bookedMap = new CustomHashMap<>();
        for (int i = 0; i < booked.size(); i++) bookedMap.put(booked.get(i), true);

        int racCount = getRACCapacity(coachTypeFromString(coachType, totalSeats));
        for (int i = 1; i <= racCount; i++) {
            String seatA = "RAC-" + i + "A";
            String seatB = "RAC-" + i + "B";
            if (!bookedMap.containsKey(seatA)) return new String[]{seatA, "RAC"};
            if (!bookedMap.containsKey(seatB)) return new String[]{seatB, "RAC"};
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEAT LAYOUT DISPLAY
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Prints a visual ASCII seat layout for a given coach on a date.
     */
    public void printSeatLayout(int trainId, int coachId, String date)
            throws DatabaseException {
        Coach coach = trainDAO.getCoachById(coachId);
        if (coach == null) {
            ConsoleUtils.printError("Coach not found: " + coachId);
            return;
        }

        CustomLinkedList<String> booked = bookingDAO.getBookedSeats(trainId, coachId, date);
        CustomHashMap<String, Boolean> bookedMap = new CustomHashMap<>();
        for (int i = 0; i < booked.size(); i++) bookedMap.put(booked.get(i), true);

        ConsoleUtils.printHeader("SEAT LAYOUT — " + coach.getCoachName()
                + " (" + coach.getCoachType() + ") | Date: " + date);
        System.out.println("  Legend: [A] = Available  [X] = Booked  [R] = RAC");
        ConsoleUtils.printLine('-');

        switch (coach.getCoachType()) {
            case "SL", "3A" -> printSleeperLayout(coach, bookedMap);
            case "2A", "1A" -> printACLayout(coach, bookedMap);
            case "CC"       -> printChairCarLayout(coach, bookedMap);
            case "GEN"      -> System.out.println("  General coach — no reserved seating.");
        }
        ConsoleUtils.printLine('-');

        int avail = getAvailableSeats(trainId, coachId, date);
        ConsoleUtils.printInfo("Available Confirmed Seats : " + avail);
        ConsoleUtils.printInfo("RAC Available             : " + isRACAvailable(trainId, coachId, date));
        ConsoleUtils.printInfo("WL Available              : " + isWLAvailable(trainId, coachId, date));
    }

    // ── Sleeper / 3A layout ───────────────────────────────────────────────────
    private void printSleeperLayout(Coach coach, CustomHashMap<String, Boolean> booked) {
        int compartments = coach.getTotalSeats() / 8;
        for (int comp = 1; comp <= compartments; comp++) {
            System.out.println("  ┌─── Compartment " + comp + " ──────────────────────┐");
            // Side berths
            String sl = "S" + comp + "-SL"; String su = "S" + comp + "-SU";
            System.out.printf("  │  SL: %s    SU: %s                  │%n",
                slot(sl, booked), slot(su, booked));
            // Main berths
            for (int b = 1; b <= 3; b++) {
                String[] labels = {"LB","MB","UB"};
                String ln = "S" + comp + "-" + b + labels[b-1];
                String rn = "S" + comp + "-" + (b+3) + labels[b-1];
                System.out.printf("  │  [%s] %-8s          [%s] %-8s │%n",
                    slot(ln, booked), ln, slot(rn, booked), rn);
            }
            System.out.println("  └─────────────────────────────────────────┘");
        }
    }

    // ── 2A / 1A layout ────────────────────────────────────────────────────────
    private void printACLayout(Coach coach, CustomHashMap<String, Boolean> booked) {
        int compartments = coach.getTotalSeats() / 4;
        for (int comp = 1; comp <= compartments; comp++) {
            System.out.println("  ┌─── Coupe " + comp + " ─────────────────┐");
            String[] berthNames = {"LB","UB","LB","UB"};
            String[] sides = {"L","L","R","R"};
            for (int b = 0; b < 4; b++) {
                String sn = "C" + comp + sides[b] + berthNames[b];
                System.out.printf("  │  [%s] %-12s          │%n", slot(sn, booked), sn);
            }
            System.out.println("  └───────────────────────────────────┘");
        }
    }

    // ── Chair car layout ─────────────────────────────────────────────────────
    private void printChairCarLayout(Coach coach, CustomHashMap<String, Boolean> booked) {
        int rows = coach.getTotalSeats() / 5;
        System.out.println("  Row  WIN   AISLE  AISLE  AISLE  WIN");
        ConsoleUtils.printLine('.');
        for (int r = 1; r <= rows; r++) {
            String a = "R" + r + "A"; String b2 = "R" + r + "B"; String c = "R" + r + "C";
            String d = "R" + r + "D"; String e = "R" + r + "E";
            System.out.printf("  %3d  [%s]   [%s]   [%s]   [%s]   [%s]%n",
                r, slot(a,booked), slot(b2,booked), slot(c,booked),
                   slot(d,booked), slot(e,booked));
        }
    }

    // ── Slot helper ──────────────────────────────────────────────────────────
    private String slot(String seat, CustomHashMap<String, Boolean> booked) {
        if (seat.startsWith("RAC")) return "R";
        return booked.containsKey(seat) ? "X" : "A";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private int getRACCapacity(Coach coach) {
        if (coach == null) return 0;
        return switch (coach.getCoachType()) {
            case "SL" -> 4;
            case "3A" -> 3;
            case "2A" -> 2;
            default   -> 0;
        };
    }

    private boolean isSleeperType(String type) {
        return "SL".equals(type) || "3A".equals(type) || "2A".equals(type);
    }

    private Coach coachTypeFromString(String type, int seats) {
        Coach c = new Coach();
        c.setCoachType(type);
        c.setTotalSeats(seats);
        return c;
    }

    /** Build flat seat-number/berth-type array for a coach type. */
    private String[] buildLayout(String coachType, int maxSeats) {
        // Each entry: index i = seat number, i+1 = berth type
        String[] result = new String[maxSeats * 2];
        int idx = 0, comp = 1;

        if ("SL".equals(coachType) || "3A".equals(coachType)) {
            while (idx < result.length) {
                for (int b = 0; b < SL_BERTHS.length && idx < result.length; b++) {
                    result[idx++] = "S" + comp + "-" + (b+1) + SL_BERTHS[b];
                    result[idx++] = SL_BERTHS[b];
                }
                comp++;
            }
        } else if ("2A".equals(coachType)) {
            while (idx < result.length) {
                for (int b = 0; b < TWO_A_BERTHS.length && idx < result.length; b++) {
                    result[idx++] = "C" + comp + (b < 3 ? "L" : "R") + TWO_A_BERTHS[b];
                    result[idx++] = TWO_A_BERTHS[b];
                }
                comp++;
            }
        } else if ("1A".equals(coachType)) {
            while (idx < result.length) {
                for (int b = 0; b < ONE_A_BERTHS.length && idx < result.length; b++) {
                    result[idx++] = "C" + comp + (b < 2 ? "L" : "R") + ONE_A_BERTHS[b];
                    result[idx++] = ONE_A_BERTHS[b];
                }
                comp++;
            }
        } else { // CC / GEN
            for (int r = 1; idx < result.length; r++) {
                char[] seats = {'A','B','C','D','E'};
                for (char s : seats) {
                    if (idx >= result.length) break;
                    result[idx++] = "R" + r + s;
                    result[idx++] = (s=='A'||s=='E') ? "WIN" : "AISLE";
                }
            }
        }
        return result;
    }
}