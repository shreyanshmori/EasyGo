package common.filehandling;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Generates text-based ticket / boarding-pass / receipt files.
 * Reusable by Railway, Flight, and Bus systems.
 *
 * Output files are saved in the ./tickets/ directory.
 * Format: plain ASCII art text files (.txt).
 */
public class TicketFileWriter {

    private static final String TICKET_DIR = "tickets/";
    private static final int    WIDTH       = 58;

    // ── Ensure ticket directory exists ────────────────────────────────────────
    static {
        java.io.File dir = new java.io.File(TICKET_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RAILWAY TICKET
    // ══════════════════════════════════════════════════════════════════════════

    public static String generateRailwayTicket(RailwayTicketData d) throws IOException {
        String filename = TICKET_DIR + "RAIL_" + d.pnr + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            line(pw, '=');
            center(pw, "INDIAN RAILWAYS — E-TICKET");
            line(pw, '=');
            kv(pw, "PNR Number",    d.pnr);
            kv(pw, "Train",         d.trainNumber + " - " + d.trainName);
            kv(pw, "From",          d.origin);
            kv(pw, "To",            d.destination);
            kv(pw, "Journey Date",  d.journeyDate);
            kv(pw, "Departure",     d.departure);
            kv(pw, "Arrival",       d.arrival);
            kv(pw, "Coach",         d.coachName + " (" + d.coachType + ")");
            kv(pw, "Status",        d.status);
            line(pw, '-');
            center(pw, "PASSENGER DETAILS");
            line(pw, '-');
            pw.println(String.format("  %-20s %-4s %-4s %-8s %-8s",
                "Name", "Age", "Sex", "Seat", "Berth"));
            line(pw, '.');
            for (int i = 0; i < d.passengerNames.length; i++) {
                pw.println(String.format("  %-20s %-4d %-4s %-8s %-8s",
                    d.passengerNames[i],
                    d.ages[i],
                    d.genders[i],
                    d.seatNumbers[i],
                    d.berthTypes[i]));
            }
            line(pw, '-');
            kv(pw, "Total Fare",    "Rs. " + String.format("%.2f", d.totalFare));
            kv(pw, "Payment",       d.paymentMethod + " | " + d.paymentStatus);
            kv(pw, "Booked On",     d.bookingDate);
            line(pw, '=');
            center(pw, "** HAVE A SAFE JOURNEY **");
            line(pw, '=');
        }
        return filename;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLIGHT BOARDING PASS
    // ══════════════════════════════════════════════════════════════════════════

    public static String generateBoardingPass(FlightTicketData d) throws IOException {
        String filename = TICKET_DIR + "FLT_" + d.bookingRef + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            line(pw, '=');
            center(pw, "BOARDING PASS");
            center(pw, d.airline.toUpperCase());
            line(pw, '=');
            kv(pw, "Booking Ref",   d.bookingRef);
            kv(pw, "Flight",        d.flightNumber);
            kv(pw, "From",          d.origin + "  ->  " + d.destination);
            kv(pw, "Date",          d.journeyDate);
            kv(pw, "Departure",     d.departure);
            kv(pw, "Arrival",       d.arrival);
            kv(pw, "Class",         d.classType);
            line(pw, '-');
            center(pw, "PASSENGER DETAILS");
            line(pw, '-');
            for (int i = 0; i < d.passengerNames.length; i++) {
                pw.println(String.format("  %-24s Seat: %-6s Gate: %s",
                    d.passengerNames[i], d.seatNumbers[i], d.gateNumber));
            }
            line(pw, '-');
            kv(pw, "Meal",          d.mealPreference);
            kv(pw, "Luggage",       d.luggageKg + " kg");
            kv(pw, "Total Fare",    "Rs. " + String.format("%.2f", d.totalFare));
            kv(pw, "Payment",       d.paymentMethod + " | " + d.paymentStatus);
            line(pw, '=');
            center(pw, "PLEASE ARRIVE 2 HOURS BEFORE DEPARTURE");
            line(pw, '=');
        }
        return filename;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BUS TICKET
    // ══════════════════════════════════════════════════════════════════════════

    public static String generateBusTicket(BusTicketData d) throws IOException {
        String filename = TICKET_DIR + "BUS_" + d.bookingRef + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            line(pw, '=');
            center(pw, "BUS TICKET");
            center(pw, d.busName.toUpperCase());
            line(pw, '=');
            kv(pw, "Booking Ref",   d.bookingRef);
            kv(pw, "Bus Number",    d.busNumber);
            kv(pw, "Bus Type",      d.busType);
            kv(pw, "From",          d.origin);
            kv(pw, "To",            d.destination);
            kv(pw, "Distance",      d.distanceKm + " km");
            kv(pw, "Journey Date",  d.journeyDate);
            kv(pw, "Departure",     d.departure);
            kv(pw, "Arrival",       d.arrival);
            line(pw, '-');
            center(pw, "PASSENGER DETAILS");
            line(pw, '-');
            pw.println(String.format("  %-22s %-4s %-4s %-8s",
                "Name", "Age", "Sex", "Seat"));
            line(pw, '.');
            for (int i = 0; i < d.passengerNames.length; i++) {
                pw.println(String.format("  %-22s %-4d %-4s %-8s",
                    d.passengerNames[i], d.ages[i], d.genders[i], d.seatNumbers[i]));
            }
            line(pw, '-');
            kv(pw, "Total Fare",    "Rs. " + String.format("%.2f", d.totalFare));
            kv(pw, "Payment",       d.paymentMethod + " | " + d.paymentStatus);
            kv(pw, "Booked On",     d.bookingDate);
            line(pw, '=');
            center(pw, "** BON VOYAGE **");
            line(pw, '=');
        }
        return filename;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PAYMENT RECEIPT
    // ══════════════════════════════════════════════════════════════════════════

    public static String generatePaymentReceipt(String bookingRef, String systemType,
            double amount, String method, String txnId, String paidAt) throws IOException {
        String filename = TICKET_DIR + "RCPT_" + bookingRef + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            line(pw, '=');
            center(pw, "PAYMENT RECEIPT");
            line(pw, '=');
            kv(pw, "Booking Ref",    bookingRef);
            kv(pw, "System",         systemType);
            kv(pw, "Amount",         "Rs. " + String.format("%.2f", amount));
            kv(pw, "Method",         method);
            kv(pw, "Transaction ID", txnId);
            kv(pw, "Paid At",        paidAt);
            kv(pw, "Status",         "PAYMENT SUCCESSFUL");
            line(pw, '=');
            center(pw, "Thank you for your booking!");
            line(pw, '=');
        }
        return filename;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static void line(PrintWriter pw, char c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < WIDTH; i++) sb.append(c);
        pw.println(sb.toString());
    }

    private static void center(PrintWriter pw, String text) {
        int pad = (WIDTH - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) sb.append(' ');
        sb.append(text);
        pw.println(sb.toString());
    }

    private static void kv(PrintWriter pw, String key, String value) {
        pw.println(String.format("  %-18s: %s", key, value));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DATA TRANSFER OBJECTS (inner static classes)
    // ══════════════════════════════════════════════════════════════════════════

    public static class RailwayTicketData {
        public String   pnr, trainNumber, trainName, origin, destination;
        public String   journeyDate, departure, arrival;
        public String   coachName, coachType, status;
        public String[] passengerNames, genders, seatNumbers, berthTypes;
        public int[]    ages;
        public double   totalFare;
        public String   paymentMethod, paymentStatus, bookingDate;
    }

    public static class FlightTicketData {
        public String   bookingRef, flightNumber, airline;
        public String   origin, destination, journeyDate, departure, arrival;
        public String   classType, gateNumber, mealPreference;
        public double   luggageKg, totalFare;
        public String[] passengerNames, seatNumbers;
        public String   paymentMethod, paymentStatus;
    }

    public static class BusTicketData {
        public String   bookingRef, busNumber, busName, busType;
        public String   origin, destination, journeyDate, departure, arrival;
        public double   distanceKm, totalFare;
        public String[] passengerNames, genders, seatNumbers;
        public int[]    ages;
        public String   paymentMethod, paymentStatus, bookingDate;
    }
}