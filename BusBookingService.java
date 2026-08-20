package bus.services;

import bus.dao.BusBookingDAO;
public class BusBookingService {

    private final bus.dao.BusDAO          busDAO;
    private final BusBookingDAO           bookingDAO;
    private final BusFareCalculator       fareCalc;
    private final BusSeatManager          seatManager;
    private final common.payment.PaymentService paymentService;

    public BusBookingService() {
        this.busDAO         = new bus.dao.BusDAO();
        this.bookingDAO     = new BusBookingDAO();
        this.fareCalc       = new BusFareCalculator();
        this.seatManager    = new BusSeatManager();
        this.paymentService = new common.payment.PaymentService();
    }

    // ── Search ────────────────────────────────────────────────────────────────
    public datastructures.linkedlist.CustomLinkedList<bus.models.Bus>
            searchBuses(String origin, String dest)
            throws common.exceptions.DatabaseException,
                   common.exceptions.BusNotFoundException {
        var list = busDAO.searchBuses(origin, dest);
        if (list.isEmpty()) throw new common.exceptions.BusNotFoundException(origin+"→"+dest);
        return list;
    }

    // ── Display buses ─────────────────────────────────────────────────────────
    public void displayBuses(datastructures.linkedlist.CustomLinkedList<bus.models.Bus> buses,
                              String date)
            throws common.exceptions.DatabaseException {
        common.utilities.ConsoleUtils.printHeader("AVAILABLE BUSES");
        String[] h = {"#","Number","Name","Type","From","To","Dep","Arr","Avail","Fare/km"};
        int[]    w = {3,8,18,11,10,10,6,6,6,9};
        common.utilities.ConsoleUtils.printTableHeader(h, w);
        for (int i = 0; i < buses.size(); i++) {
            bus.models.Bus b = buses.get(i);
            int avail = seatManager.getAvailableCount(b.getBusId(), date, b.getTotalSeats());
            common.utilities.ConsoleUtils.printTableRow(new String[]{
                String.valueOf(i+1), b.getBusNumber(), b.getBusName(),
                b.getBusType(), b.getOrigin(), b.getDestination(),
                b.getDeparture(), b.getArrival(),
                String.valueOf(avail), String.format("%.2f", b.getFarePerKm())
            }, w);
        }
        common.utilities.ConsoleUtils.printTableSeparator(w);
    }

    // ── Book ──────────────────────────────────────────────────────────────────
    public String bookBus(int userId, int busId, String date,
                          datastructures.linkedlist.CustomLinkedList<bus.models.BusPassenger> passengers,
                          boolean preferLower)
            throws common.exceptions.ReservationException {

        bus.models.Bus bus = busDAO.getById(busId);
        if (bus == null) throw new common.exceptions.BusNotFoundException("ID:" + busId);
        common.validation.Validator.futureDate(date);

        int pCount = passengers.size();
        int avail  = seatManager.getAvailableCount(busId, date, bus.getTotalSeats());
        if (avail < pCount)
            throw new common.exceptions.SeatNotAvailableException(
                "Only " + avail + " seats available.");

        double totalFare = fareCalc.calculateTotalFare(
            bus.getDistanceKm(), bus.getFarePerKm(), bus.getBusType(), pCount);

        String ref = common.utilities.AuthManager.generateBookingRef("BUS");

        common.database.DBConnection db = common.database.DBConnection.getInstance();
        int bookingId;
        try {
            db.beginTransaction();
            bookingId = bookingDAO.insertBooking(ref, userId, busId, date, "CONFIRMED", totalFare);

            for (int i = 0; i < pCount; i++) {
                bus.models.BusPassenger p = passengers.get(i);
                String preferred = preferLower ? "LOWER" : "WINDOW";
                String seat = seatManager.assignPreferredSeat(busId, date, preferred);
                if (seat == null) seat = "S" + (bookingId + i);
                bookingDAO.insertPassenger(bookingId, p.getName(), p.getAge(),
                    p.getGender(), seat, p.getIdType(), p.getIdNumber());
                p.setSeatNumber(seat);
            }
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new common.exceptions.ReservationException("BOOKING_FAILED",
                "Bus booking failed: " + e.getMessage(), e);
        }

        // Payment
        try {
            paymentService.pay(ref, "BUS", totalFare);
        } catch (common.exceptions.PaymentFailedException e) {
            try { db.beginTransaction(); bookingDAO.updateStatus(bookingId,"CANCELLED"); db.commit(); }
            catch (Exception ex) { db.rollback(); }
            throw e;
        }

        fareCalc.printFareBreakdown(bus.getDistanceKm(), bus.getFarePerKm(),
            bus.getBusType(), pCount);
        common.utilities.ConsoleUtils.printSuccess(
            "Bus booked! Ref: " + ref + " | Fare: Rs." + totalFare);
        return ref;
    }

    // ── Cancel ────────────────────────────────────────────────────────────────
    public void cancelBooking(String ref)
            throws common.exceptions.ReservationException {
        bus.models.BusBooking b = bookingDAO.getByRef(ref);
        if (b == null) throw new common.exceptions.BookingNotFoundException(ref);
        if ("CANCELLED".equals(b.getStatus()))
            throw new common.exceptions.CancellationException("Already cancelled: " + ref);

        common.database.DBConnection db = common.database.DBConnection.getInstance();
        try {
            db.beginTransaction();
            bookingDAO.updateStatus(b.getBookingId(), "CANCELLED");
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new common.exceptions.CancellationException("Cancel failed: " + e.getMessage());
        }

        try {
            paymentService.refund(ref, "BUS", b.getJourneyDate(), b.getBusType());
        } catch (Exception e) {
            common.utilities.ConsoleUtils.printWarning("Refund failed: " + e.getMessage());
        }
        common.utilities.ConsoleUtils.printSuccess("Booking " + ref + " cancelled.");
    }

    // ── History ───────────────────────────────────────────────────────────────
    public void showHistory(int userId) throws common.exceptions.DatabaseException {
        var list = bookingDAO.getByUser(userId);
        common.utilities.ConsoleUtils.printHeader("MY BUS BOOKINGS");
        if (list.isEmpty()) { common.utilities.ConsoleUtils.printInfo("No bookings."); return; }
        String[] h = {"Ref","Bus","From→To","Date","Status","Fare"};
        int[]    w = {14,18,20,12,10,10};
        common.utilities.ConsoleUtils.printTableHeader(h, w);
        for (int i = 0; i < list.size(); i++) {
            bus.models.BusBooking b = list.get(i);
            common.utilities.ConsoleUtils.printTableRow(new String[]{
                b.getBookingRef(), b.getBusName(),
                b.getOrigin()+"→"+b.getDestination(),
                b.getJourneyDate(), b.getStatus(),
                String.format("%.2f", b.getTotalFare())
            }, w);
        }
        common.utilities.ConsoleUtils.printTableSeparator(w);
    }

    // ── Booking details ───────────────────────────────────────────────────────
    public void showDetails(String ref) throws common.exceptions.DatabaseException {
        bus.models.BusBooking b = bookingDAO.getByRef(ref);
        if (b == null) { common.utilities.ConsoleUtils.printError("Not found: " + ref); return; }
        common.utilities.ConsoleUtils.printHeader("BOOKING — " + ref);
        common.utilities.ConsoleUtils.printInfo("Bus     : " + b.getBusNumber()+" "+b.getBusName());
        common.utilities.ConsoleUtils.printInfo("Route   : " + b.getOrigin()+" → "+b.getDestination());
        common.utilities.ConsoleUtils.printInfo("Type    : " + b.getBusType());
        common.utilities.ConsoleUtils.printInfo("Date    : " + b.getJourneyDate());
        common.utilities.ConsoleUtils.printInfo("Dep/Arr : " + b.getDeparture()+" / "+b.getArrival());
        common.utilities.ConsoleUtils.printInfo("Status  : " + b.getStatus());
        common.utilities.ConsoleUtils.printInfo(String.format("Fare    : Rs. %.2f", b.getTotalFare()));
        common.utilities.ConsoleUtils.printLine('-');
        var passengers = bookingDAO.getPassengers(b.getBookingId());
        String[] h = {"Name","Age","Gender","Seat"};
        int[]    w = {24,5,7,8};
        common.utilities.ConsoleUtils.printTableHeader(h, w);
        for (int i = 0; i < passengers.size(); i++) {
            bus.models.BusPassenger p = passengers.get(i);
            common.utilities.ConsoleUtils.printTableRow(new String[]{
                p.getName(), String.valueOf(p.getAge()),
                p.getGender(), p.getSeatNumber()
            }, w);
        }
        common.utilities.ConsoleUtils.printTableSeparator(w);
    }

    public BusFareCalculator getFareCalc()   { return fareCalc; }
    public BusSeatManager    getSeatManager(){ return seatManager; }
    BusBookingDAO            getBookingDAO() { return bookingDAO; }
}


