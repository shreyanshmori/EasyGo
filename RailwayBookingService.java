package railway.services;

import common.database.DBConnection;
import common.exceptions.*;
import common.payment.PaymentService;
import common.payment.PaymentModel;
import common.utilities.AuthManager;
import common.utilities.ConsoleUtils;
import common.utilities.DateUtils;
import common.validation.Validator;
import common.filehandling.TicketFileWriter;
import datastructures.linkedlist.CustomLinkedList;
import datastructures.queue.CustomQueue;
import railway.dao.BookingDAO;
import railway.dao.TrainDAO;
import railway.dao.WaitingListDAO;
import railway.management.SeatManager;
import railway.models.*;

/**
 * RailwayBookingService — core booking business logic.
 *
 * Flow:
 *   searchTrain → selectCoach → collectPassengers
 *   → assignSeats (CONFIRMED / RAC / WL) → processPayment → generateTicket
 *
 * Cancellation flow:
 *   cancelByPNR → refundPolicy → promoteWL/RAC → updateStatus
 */
public class RailwayBookingService {

    private final TrainDAO       trainDAO;
    private final BookingDAO     bookingDAO;
    private final WaitingListDAO wlDAO;
    private final SeatManager    seatManager;
    private final PaymentService paymentService;

    // In-memory WL queue per "trainId_coachId_date" (mirrors DB)
    private final datastructures.hashmap.CustomHashMap<String, CustomQueue<Integer>> wlQueues;

    public RailwayBookingService() {
        this.trainDAO       = new TrainDAO();
        this.bookingDAO     = new BookingDAO();
        this.wlDAO          = new WaitingListDAO();
        this.seatManager    = new SeatManager();
        this.paymentService = new PaymentService();
        this.wlQueues       = new datastructures.hashmap.CustomHashMap<>();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEARCH TRAINS
    // ══════════════════════════════════════════════════════════════════════════

    public CustomLinkedList<Train> searchTrains(String origin, String destination)
            throws DatabaseException, TrainNotFoundException {
        CustomLinkedList<Train> trains = trainDAO.searchTrains(origin, destination);
        if (trains.isEmpty())
            throw new TrainNotFoundException(origin + " → " + destination);
        return trains;
    }

    public void displayTrains(CustomLinkedList<Train> trains) {
        String[] headers = {"#", "Number", "Name", "From", "To", "Dep", "Arr", "Status"};
        int[]    widths  = {3, 8, 26, 12, 12, 8, 8, 8};
        ConsoleUtils.printTableHeader(headers, widths);
        for (int i = 0; i < trains.size(); i++) {
            Train t = trains.get(i);
            ConsoleUtils.printTableRow(new String[]{
                String.valueOf(i+1), t.getTrainNumber(), t.getTrainName(),
                t.getOrigin(), t.getDestination(),
                t.getDeparture(), t.getArrival(), t.getStatus()
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  COACH & AVAILABILITY
    // ══════════════════════════════════════════════════════════════════════════

    public void displayCoachAvailability(int trainId, String date) throws DatabaseException {
        CustomLinkedList<Coach> coaches = trainDAO.getCoachesByTrain(trainId);
        ConsoleUtils.printHeader("COACH AVAILABILITY");
        String[] headers = {"#", "Coach", "Type", "Seats", "Available", "RAC", "WL", "Fare/km"};
        int[]    widths  = {3, 7, 5, 6, 10, 5, 5, 10};
        ConsoleUtils.printTableHeader(headers, widths);
        for (int i = 0; i < coaches.size(); i++) {
            Coach c = coaches.get(i);
            int avail = seatManager.getAvailableSeats(trainId, c.getCoachId(), date);
            boolean rac = seatManager.isRACAvailable(trainId, c.getCoachId(), date);
            boolean wl  = seatManager.isWLAvailable(trainId, c.getCoachId(), date);
            ConsoleUtils.printTableRow(new String[]{
                String.valueOf(i+1), c.getCoachName(), c.getCoachType(),
                String.valueOf(c.getTotalSeats()), String.valueOf(avail),
                rac ? "Y" : "N", wl ? "Y" : "N",
                String.format("%.2f", c.getFarePerKm())
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOK TICKET
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Full booking flow for a logged-in user.
     */
    public String bookTicket(int userId, int trainId, int coachId,
                             String journeyDate, CustomLinkedList<Passenger> passengers)
            throws ReservationException {

        Train train = trainDAO.getTrainById(trainId);
        Coach coach = trainDAO.getCoachById(coachId);
        if (train == null) throw new TrainNotFoundException("ID:" + trainId);
        if (coach == null) throw new ValidationException("coach", "Invalid coach selected");

        // Validate date
        Validator.futureDate(journeyDate);

        // Calculate fare
        double fare = calculateFare(coach.getFarePerKm(), train.getDistanceKm(),
                                     passengers.size());

        // Determine booking status
        int avail = seatManager.getAvailableSeats(trainId, coachId, journeyDate);
        String bookingStatus;
        if (avail >= passengers.size())               bookingStatus = "CONFIRMED";
        else if (seatManager.isRACAvailable(trainId, coachId, journeyDate)) bookingStatus = "RAC";
        else if (seatManager.isWLAvailable(trainId, coachId, journeyDate))  bookingStatus = "WAITING";
        else throw new SeatNotAvailableException("No seats / WL / RAC available for the selected coach.");

        String pnr = AuthManager.generatePNR();

        // ── JDBC Transaction ──────────────────────────────────────────────────
        DBConnection db = DBConnection.getInstance();
        int bookingId;
        try {
            db.beginTransaction();

            bookingId = bookingDAO.insertBooking(pnr, userId, trainId, coachId,
                                                  journeyDate, bookingStatus, fare);

            // Assign seats and insert passengers
            for (int i = 0; i < passengers.size(); i++) {
                Passenger p = passengers.get(i);
                String seat = ""; String berth = "";

                if ("CONFIRMED".equals(bookingStatus)) {
                    String[] assigned = seatManager.assignNextSeat(
                            trainId, coachId, journeyDate,
                            coach.getCoachType(), coach.getTotalSeats());
                    if (assigned != null) { seat = assigned[0]; berth = assigned[1]; }
                } else if ("RAC".equals(bookingStatus)) {
                    String[] assigned = seatManager.assignRACseat(
                            trainId, coachId, journeyDate,
                            coach.getCoachType(), coach.getTotalSeats());
                    if (assigned != null) { seat = assigned[0]; berth = "RAC"; }
                } else {
                    seat = "WL"; berth = "WL";
                }

                bookingDAO.insertPassenger(bookingId, p.getName(), p.getAge(),
                        p.getGender(), seat, berth, p.getIdType(), p.getIdNumber());
            }

            // WL/RAC queue entries
            if ("WAITING".equals(bookingStatus)) {
                int wlPos = wlDAO.getWLCount(trainId, coachId, journeyDate) + 1;
                wlDAO.insertWL(bookingId, trainId, coachId, journeyDate, wlPos);
                addToWLQueue(trainId, coachId, journeyDate, bookingId);
            } else if ("RAC".equals(bookingStatus)) {
                int racPos = wlDAO.getRACCount(trainId, coachId, journeyDate) + 1;
                String racSeat = "RAC-" + racPos;
                wlDAO.insertRAC(bookingId, trainId, coachId, journeyDate, racPos, racSeat);
            }

            db.commit();

        } catch (Exception e) {
            db.rollback();
            throw new ReservationException("BOOKING_FAILED", "Booking failed: " + e.getMessage(), e);
        }

        // ── Process payment ───────────────────────────────────────────────────
        try {
            paymentService.pay(pnr, "RAILWAY", fare);
        } catch (PaymentFailedException e) {
            // Rollback booking on payment failure
            try {
                db.beginTransaction();
                bookingDAO.updateStatus(bookingId, "CANCELLED");
                db.commit();
            } catch (Exception ex) { db.rollback(); }
            throw e;
        }

        // ── Generate ticket file ──────────────────────────────────────────────
        generateTicketFile(bookingId, pnr, train, coach, journeyDate,
                           passengers, fare, bookingStatus);

        ConsoleUtils.printSuccess("Booking confirmed! PNR: " + pnr
                + " | Status: " + bookingStatus);
        return pnr;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CANCEL TICKET
    // ══════════════════════════════════════════════════════════════════════════

    public void cancelByPNR(String pnr) throws ReservationException {
        Booking booking = bookingDAO.getByPNR(pnr);
        if (booking == null) throw new BookingNotFoundException(pnr);
        if ("CANCELLED".equals(booking.getStatus()))
            throw new CancellationException("Booking " + pnr + " is already cancelled.");

        DBConnection db = DBConnection.getInstance();
        try {
            db.beginTransaction();

            // Update booking status
            bookingDAO.updateStatus(booking.getBookingId(), "CANCELLED");

            // Promote WL → RAC → CONFIRMED
            if ("CONFIRMED".equals(booking.getStatus())) {
                promoteRACToConfirmed(booking);
            } else if ("RAC".equals(booking.getStatus())) {
                wlDAO.deleteRAC(booking.getBookingId());
                promoteWLToRAC(booking);
            } else if ("WAITING".equals(booking.getStatus())) {
                wlDAO.deleteWL(booking.getBookingId());
                wlDAO.shiftWLPositions(booking.getTrainId(), booking.getCoachId(),
                                       booking.getJourneyDate());
            }

            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new CancellationException("Cancellation failed: " + e.getMessage());
        }

        // Process refund
        try {
            paymentService.refund(pnr, "RAILWAY",
                    booking.getJourneyDate(), booking.getStatus());
        } catch (PaymentFailedException | DatabaseException e) {
            ConsoleUtils.printWarning("Refund processing failed: " + e.getMessage());
        }

        ConsoleUtils.printSuccess("Booking " + pnr + " cancelled successfully.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PNR STATUS
    // ══════════════════════════════════════════════════════════════════════════

    public void checkPNRStatus(String pnr) throws ReservationException {
        Booking booking = bookingDAO.getByPNR(pnr);
        if (booking == null) throw new BookingNotFoundException(pnr);

        ConsoleUtils.printHeader("PNR STATUS — " + pnr);
        ConsoleUtils.printInfo("Train        : " + booking.getTrainNumber()
                + " - " + booking.getTrainName());
        ConsoleUtils.printInfo("Route        : " + booking.getOrigin()
                + " → " + booking.getDestination());
        ConsoleUtils.printInfo("Journey Date : " + booking.getJourneyDate());
        ConsoleUtils.printInfo("Coach        : " + booking.getCoachName()
                + " (" + booking.getCoachType() + ")");
        ConsoleUtils.printInfo("Status       : " + booking.getStatus());
        ConsoleUtils.printInfo(String.format("Fare         : Rs. %.2f", booking.getTotalFare()));
        ConsoleUtils.printLine('-');
        ConsoleUtils.printInfo("PASSENGERS:");

        CustomLinkedList<Passenger> passengers;
        try {
            passengers = bookingDAO.getPassengers(booking.getBookingId());
        } catch (DatabaseException e) {
            throw new ReservationException("DB_ERROR", e.getMessage());
        }

        String[] headers = {"Name", "Age", "Gender", "Seat", "Berth"};
        int[]    widths  = {24, 5, 7, 10, 6};
        ConsoleUtils.printTableHeader(headers, widths);
        for (int i = 0; i < passengers.size(); i++) {
            Passenger p = passengers.get(i);
            ConsoleUtils.printTableRow(new String[]{
                p.getName(), String.valueOf(p.getAge()),
                p.getGender(), p.getSeatNumber(), p.getBerthType()
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);

        // Show WL/RAC position if applicable
        try {
            if ("WAITING".equals(booking.getStatus())) {
                int wlCount = wlDAO.getWLCount(booking.getTrainId(),
                        booking.getCoachId(), booking.getJourneyDate());
                ConsoleUtils.printInfo("Waiting List Position: WL/" + wlCount);
            } else if ("RAC".equals(booking.getStatus())) {
                int racCount = wlDAO.getRACCount(booking.getTrainId(),
                        booking.getCoachId(), booking.getJourneyDate());
                ConsoleUtils.printInfo("RAC Position: RAC/" + racCount);
            }
        } catch (DatabaseException ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOKING HISTORY
    // ══════════════════════════════════════════════════════════════════════════

    public void showBookingHistory(int userId) throws DatabaseException {
        CustomLinkedList<Booking> bookings = bookingDAO.getByUser(userId);
        ConsoleUtils.printHeader("MY RAILWAY BOOKINGS");
        if (bookings.isEmpty()) {
            ConsoleUtils.printInfo("No bookings found.");
            return;
        }
        String[] headers = {"PNR", "Train", "From→To", "Date", "Status", "Fare"};
        int[]    widths  = {16, 20, 22, 12, 10, 10};
        ConsoleUtils.printTableHeader(headers, widths);
        for (int i = 0; i < bookings.size(); i++) {
            Booking b = bookings.get(i);
            ConsoleUtils.printTableRow(new String[]{
                b.getPnr(), b.getTrainName(),
                b.getOrigin() + "→" + b.getDestination(),
                b.getJourneyDate(), b.getStatus(),
                String.format("%.2f", b.getTotalFare())
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private double calculateFare(double farePerKm, double distanceKm, int passengers) {
        double base = farePerKm * distanceKm;
        double tax  = base * 0.05;  // 5% GST
        return Math.round((base + tax) * passengers * 100.0) / 100.0;
    }

    private void promoteRACToConfirmed(Booking cancelled) throws DatabaseException {
        RACEntry rac = wlDAO.getFirstRAC(cancelled.getTrainId(),
                cancelled.getCoachId(), cancelled.getJourneyDate());
        if (rac == null) return;

        // Promote RAC booking to CONFIRMED
        bookingDAO.updateStatus(rac.getBookingId(), "CONFIRMED");
        // Assign freed seat
        CustomLinkedList<Passenger> passengers = bookingDAO.getPassengers(rac.getBookingId());
        if (!passengers.isEmpty()) {
            Coach coach = trainDAO.getCoachById(cancelled.getCoachId());
            String[] seat = seatManager.assignNextSeat(cancelled.getTrainId(),
                    cancelled.getCoachId(), cancelled.getJourneyDate(),
                    coach.getCoachType(), coach.getTotalSeats());
            if (seat != null)
                bookingDAO.updatePassengerSeat(passengers.get(0).getPassengerId(),
                        seat[0], seat[1]);
        }
        wlDAO.deleteRAC(rac.getBookingId());
        // Now promote first WL to RAC
        promoteWLToRAC(cancelled);
        ConsoleUtils.printInfo("RAC/" + rac.getRacPosition()
                + " promoted to CONFIRMED (Booking ID: " + rac.getBookingId() + ")");
    }

    private void promoteWLToRAC(Booking reference) throws DatabaseException {
        WaitingListEntry wl = wlDAO.getFirstWL(reference.getTrainId(),
                reference.getCoachId(), reference.getJourneyDate());
        if (wl == null) return;

        bookingDAO.updateStatus(wl.getBookingId(), "RAC");
        int newRacPos = wlDAO.getRACCount(reference.getTrainId(),
                reference.getCoachId(), reference.getJourneyDate()) + 1;
        String racSeat = "RAC-" + newRacPos;
        wlDAO.insertRAC(wl.getBookingId(), wl.getTrainId(),
                wl.getCoachId(), wl.getJourneyDate(), newRacPos, racSeat);

        // Update passenger seat to RAC
        CustomLinkedList<Passenger> passengers = bookingDAO.getPassengers(wl.getBookingId());
        if (!passengers.isEmpty())
            bookingDAO.updatePassengerSeat(passengers.get(0).getPassengerId(), racSeat, "RAC");

        wlDAO.deleteWL(wl.getBookingId());
        wlDAO.shiftWLPositions(wl.getTrainId(), wl.getCoachId(), wl.getJourneyDate());
        ConsoleUtils.printInfo("WL/1 promoted to RAC (Booking ID: " + wl.getBookingId() + ")");
    }

    private void addToWLQueue(int trainId, int coachId, String date, int bookingId) {
        String key = trainId + "_" + coachId + "_" + date;
        CustomQueue<Integer> q = wlQueues.get(key);
        if (q == null) { q = new CustomQueue<>(); wlQueues.put(key, q); }
        q.enqueue(bookingId);
    }

    private void generateTicketFile(int bookingId, String pnr, Train train,
                                     Coach coach, String journeyDate,
                                     CustomLinkedList<Passenger> passengers,
                                     double fare, String status) {
        try {
            CustomLinkedList<Passenger> dbPassengers = bookingDAO.getPassengers(bookingId);
            int n = dbPassengers.size();
            String[] names   = new String[n];
            int[]    ages    = new int[n];
            String[] genders = new String[n];
            String[] seats   = new String[n];
            String[] berths  = new String[n];
            for (int i = 0; i < n; i++) {
                Passenger p = dbPassengers.get(i);
                names[i]   = p.getName();
                ages[i]    = p.getAge();
                genders[i] = p.getGender();
                seats[i]   = p.getSeatNumber() != null ? p.getSeatNumber() : "-";
                berths[i]  = p.getBerthType()  != null ? p.getBerthType()  : "-";
            }
            TicketFileWriter.RailwayTicketData d = new TicketFileWriter.RailwayTicketData();
            d.pnr            = pnr;
            d.trainNumber    = train.getTrainNumber();
            d.trainName      = train.getTrainName();
            d.origin         = train.getOrigin();
            d.destination    = train.getDestination();
            d.journeyDate    = journeyDate;
            d.departure      = train.getDeparture();
            d.arrival        = train.getArrival();
            d.coachName      = coach.getCoachName();
            d.coachType      = coach.getCoachType();
            d.status         = status;
            d.passengerNames = names;
            d.ages           = ages;
            d.genders        = genders;
            d.seatNumbers    = seats;
            d.berthTypes     = berths;
            d.totalFare      = fare;
            d.paymentMethod  = "PAID";
            d.paymentStatus  = "SUCCESS";
            d.bookingDate    = DateUtils.today();

            String file = TicketFileWriter.generateRailwayTicket(d);
            ConsoleUtils.printInfo("Ticket saved: " + file);
        } catch (Exception e) {
            ConsoleUtils.printWarning("Ticket file generation failed: " + e.getMessage());
        }
    }
}