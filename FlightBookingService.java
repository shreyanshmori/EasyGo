package flight.services;

import common.database.DBConnection;
import common.exceptions.*;
import common.payment.PaymentService;
import common.utilities.AuthManager;
import common.utilities.ConsoleUtils;
import common.utilities.DateUtils;
import common.validation.Validator;
import datastructures.linkedlist.CustomLinkedList;
import flight.dao.FlightBookingDAO;
import flight.dao.FlightDAO;
import flight.models.*;

/**
 * FlightBookingService — core booking logic for the Flight system.
 *
 * Flow:
 *   searchFlight → selectClass → collectPassengers
 *   → meal selection → luggage declaration
 *   → dynamic pricing → payment → boarding pass
 *
 * Cancellation:
 *   cancelByRef → refund (class-based policy) → seat release
 */
public class FlightBookingService {

    private final FlightDAO           flightDAO;
    private final FlightBookingDAO    bookingDAO;
    private final DynamicPricingService pricing;
    private final PaymentService      paymentService;

    public FlightBookingService() {
        this.flightDAO      = new FlightDAO();
        this.bookingDAO     = new FlightBookingDAO();
        this.pricing        = new DynamicPricingService();
        this.paymentService = new PaymentService();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEARCH
    // ══════════════════════════════════════════════════════════════════════════

    public CustomLinkedList<Flight> searchFlights(String origin, String dest)
            throws DatabaseException, FlightNotFoundException {
        CustomLinkedList<Flight> list = flightDAO.searchFlights(origin, dest);
        if (list.isEmpty()) throw new FlightNotFoundException(origin + " → " + dest);
        return list;
    }

    public void displayFlights(CustomLinkedList<Flight> flights, String date)
            throws DatabaseException {
        ConsoleUtils.printHeader("AVAILABLE FLIGHTS");
        String[] headers = {"#","Flight","Airline","From","To","Dep","Arr","Eco","Biz","1st"};
        int[]    widths  = {3,8,14,10,10,6,6,5,5,5};
        ConsoleUtils.printTableHeader(headers, widths);

        for (int i = 0; i < flights.size(); i++) {
            Flight f = flights.get(i);
            int eAvail = availableSeats(f.getFlightId(), "ECONOMY",  date);
            int bAvail = availableSeats(f.getFlightId(), "BUSINESS", date);
            int fAvail = availableSeats(f.getFlightId(), "FIRST",    date);
            ConsoleUtils.printTableRow(new String[]{
                String.valueOf(i+1), f.getFlightNumber(), f.getAirline(),
                f.getOrigin(), f.getDestination(),
                f.getDeparture(), f.getArrival(),
                String.valueOf(eAvail), String.valueOf(bAvail), String.valueOf(fAvail)
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOK FLIGHT
    // ══════════════════════════════════════════════════════════════════════════

    public String bookFlight(int userId, int flightId, String classType,
                             String journeyDate,
                             CustomLinkedList<FlightPassenger> passengers,
                             CustomLinkedList<String> mealChoices,
                             CustomLinkedList<Double> luggageWeights)
            throws ReservationException {

        Flight flight = flightDAO.getById(flightId);
        if (flight == null) throw new FlightNotFoundException("ID:" + flightId);

        Validator.futureDate(journeyDate);

        int pCount = passengers.size();
        int avail  = availableSeats(flightId, classType, journeyDate);

        if (avail < pCount)
            throw new SeatNotAvailableException(
                "Only " + avail + " " + classType + " seats available.");

        // Dynamic fare
        double totalFare = pricing.calculateFare(
            flightId, flight.getDistanceKm(), classType,
            journeyDate, avail, pCount);

        // Add luggage surcharges
        double luggageCharge = 0;
        for (int i = 0; i < luggageWeights.size(); i++)
            luggageCharge += LuggageInfo.calculateExtra(luggageWeights.get(i), classType);
        totalFare += luggageCharge;

        // Add meal charges (business/first free, economy Rs.200/meal)
        double mealCharge = 0;
        if ("ECONOMY".equalsIgnoreCase(classType)) {
            for (int i = 0; i < mealChoices.size(); i++) {
                String m = mealChoices.get(i);
                if (m != null && !m.equalsIgnoreCase("NONE")) mealCharge += 200.0;
            }
        }
        totalFare += mealCharge;
        totalFare  = Math.round(totalFare * 100.0) / 100.0;

        String bookingRef = AuthManager.generateBookingRef("FLT");

        // ── JDBC Transaction ──────────────────────────────────────────────────
        DBConnection db = DBConnection.getInstance();
        int bookingId;
        try {
            db.beginTransaction();

            bookingId = bookingDAO.insertBooking(
                bookingRef, userId, flightId, journeyDate,
                classType, "CONFIRMED", totalFare);

            for (int i = 0; i < pCount; i++) {
                FlightPassenger p = passengers.get(i);

                // Assign next available seat
                String seat = flightDAO.getAvailableSeat(flightId, classType, journeyDate);
                if (seat == null) seat = classType.charAt(0) + String.valueOf(bookingId + i);

                int pId = bookingDAO.insertPassenger(
                    bookingId, p.getName(), p.getAge(), p.getGender(),
                    seat, p.getPassportNo(), p.getNationality());

                // Meal
                String mealType = (i < mealChoices.size()) ? mealChoices.get(i) : "VEG";
                double mCharge  = "ECONOMY".equalsIgnoreCase(classType) &&
                                  mealType != null && !mealType.equalsIgnoreCase("NONE")
                                  ? 200.0 : 0.0;
                bookingDAO.insertMeal(pId, bookingId,
                    mealType != null ? mealType : "VEG", mCharge);

                // Luggage
                double kg    = (i < luggageWeights.size()) ? luggageWeights.get(i) : 15.0;
                double extra = LuggageInfo.calculateExtra(kg, classType);
                bookingDAO.insertLuggage(pId, bookingId, kg, extra);

                // Store seat back on passenger object for boarding pass
                p.setSeatNumber(seat);
                p.setMealType(mealType);
                p.setLuggageKg(kg);
            }

            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new ReservationException("BOOKING_FAILED",
                "Flight booking failed: " + e.getMessage(), e);
        }

        // ── Payment ───────────────────────────────────────────────────────────
        try {
            paymentService.pay(bookingRef, "FLIGHT", totalFare);
        } catch (PaymentFailedException e) {
            try {
                db.beginTransaction();
                bookingDAO.updateStatus(bookingId, "CANCELLED");
                db.commit();
            } catch (Exception ex) { db.rollback(); }
            throw e;
        }

        // ── Show fare breakdown ───────────────────────────────────────────────
        pricing.printFareBreakdown(flightId, flight.getDistanceKm(),
            classType, journeyDate, avail, pCount);

        ConsoleUtils.printSuccess("Flight booked! Ref: " + bookingRef
            + " | Class: " + classType + " | Fare: Rs." + totalFare);

        return bookingRef;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CANCEL
    // ══════════════════════════════════════════════════════════════════════════

    public void cancelBooking(String bookingRef) throws ReservationException {
        FlightBooking b = bookingDAO.getByRef(bookingRef);
        if (b == null) throw new BookingNotFoundException(bookingRef);
        if ("CANCELLED".equals(b.getStatus()))
            throw new CancellationException("Booking " + bookingRef + " already cancelled.");

        DBConnection db = DBConnection.getInstance();
        try {
            db.beginTransaction();
            bookingDAO.updateStatus(b.getBookingId(), "CANCELLED");
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new CancellationException("Cancellation failed: " + e.getMessage());
        }

        // Invalidate pricing cache for freed seats
        pricing.invalidateCache(b.getFlightId(), b.getClassType(), b.getJourneyDate());

        // Refund
        try {
            paymentService.refund(bookingRef, "FLIGHT",
                b.getJourneyDate(), b.getClassType());
        } catch (Exception e) {
            ConsoleUtils.printWarning("Refund failed: " + e.getMessage());
        }

        ConsoleUtils.printSuccess("Booking " + bookingRef + " cancelled.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOKING DETAILS
    // ══════════════════════════════════════════════════════════════════════════

    public void showBookingDetails(String ref) throws DatabaseException {
        FlightBooking b = bookingDAO.getByRef(ref);
        if (b == null) { ConsoleUtils.printError("Booking not found: " + ref); return; }

        ConsoleUtils.printHeader("BOOKING DETAILS — " + ref);
        ConsoleUtils.printInfo("Flight    : " + b.getFlightNumber() + " (" + b.getAirline() + ")");
        ConsoleUtils.printInfo("Route     : " + b.getOrigin() + " → " + b.getDestination());
        ConsoleUtils.printInfo("Date      : " + DateUtils.friendly(b.getJourneyDate()));
        ConsoleUtils.printInfo("Departure : " + b.getDeparture());
        ConsoleUtils.printInfo("Arrival   : " + b.getArrival());
        ConsoleUtils.printInfo("Class     : " + b.getClassType());
        ConsoleUtils.printInfo("Status    : " + b.getStatus());
        ConsoleUtils.printInfo(String.format("Fare      : Rs. %.2f", b.getTotalFare()));
        ConsoleUtils.printLine('-');

        CustomLinkedList<FlightPassenger> passengers = bookingDAO.getPassengers(b.getBookingId());
        String[] h = {"Name","Age","Gender","Seat","Passport","Nationality"};
        int[]    w = {22,4,7,6,12,14};
        ConsoleUtils.printTableHeader(h, w);
        for (int i = 0; i < passengers.size(); i++) {
            FlightPassenger p = passengers.get(i);
            ConsoleUtils.printTableRow(new String[]{
                p.getName(), String.valueOf(p.getAge()), p.getGender(),
                nvl(p.getSeatNumber()), nvl(p.getPassportNo()), nvl(p.getNationality())
            }, w);
        }
        ConsoleUtils.printTableSeparator(w);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOKING HISTORY
    // ══════════════════════════════════════════════════════════════════════════

    public void showHistory(int userId) throws DatabaseException {
        CustomLinkedList<FlightBooking> list = bookingDAO.getByUser(userId);
        ConsoleUtils.printHeader("MY FLIGHT BOOKINGS");
        if (list.isEmpty()) { ConsoleUtils.printInfo("No bookings found."); return; }

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
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    public int availableSeats(int flightId, String classType, String date)
            throws DatabaseException {
        int total  = flightDAO.countSeatsByClass(flightId, classType);
        int booked = flightDAO.countBookedByClass(flightId, classType, date);
        return Math.max(0, total - booked);
    }

    public DynamicPricingService getPricingService() { return pricing; }

    FlightBookingDAO getBookingDAO() { return bookingDAO; }

    private String nvl(String s) { return (s == null || s.isEmpty()) ? "-" : s; }
}