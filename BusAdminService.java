package bus.services;

import bus.dao.BusBookingDAO;
public class BusAdminService {

    private final bus.dao.BusDAO      busDAO;
    private final BusBookingDAO       bookingDAO;
    private final BusFareCalculator   fareCalc;
    private final BusSeatManager      seatManager;

    public BusAdminService() {
        this.busDAO      = new bus.dao.BusDAO();
        this.bookingDAO  = new BusBookingDAO();
        this.fareCalc    = new BusFareCalculator();
        this.seatManager = new BusSeatManager();
    }

    // ── Add bus ───────────────────────────────────────────────────────────────
    public void addBusInteractive() throws common.exceptions.DatabaseException {
        common.utilities.ConsoleUtils.printHeader("ADD NEW BUS");
        String number  = common.utilities.ConsoleUtils.readStringNonEmpty("Bus Number");
        String name    = common.utilities.ConsoleUtils.readStringNonEmpty("Bus Name");
        String origin  = common.utilities.ConsoleUtils.readStringNonEmpty("Origin");
        String dest    = common.utilities.ConsoleUtils.readStringNonEmpty("Destination");
        double dist    = common.utilities.ConsoleUtils.readDouble("Distance (km)");
        String dep     = common.utilities.ConsoleUtils.readStringNonEmpty("Departure Time");
        String arr     = common.utilities.ConsoleUtils.readStringNonEmpty("Arrival Time");
        String[] types = {"SEATER","SLEEPER","AC_SEATER","AC_SLEEPER"};
        int tc         = common.utilities.ConsoleUtils.showMenu("Bus Type", types);
        if (tc == 0) return;
        String type    = types[tc - 1];
        int    seats   = common.utilities.ConsoleUtils.readInt("Total Seats");
        double fare    = common.utilities.ConsoleUtils.readDouble("Fare per km (Rs.)");

        common.database.DBConnection db = common.database.DBConnection.getInstance();
        try {
            db.beginTransaction();
            int routeId = busDAO.insertRoute(origin, dest, dist);
            int busId   = busDAO.insertBus(number, name, routeId, type, dep, arr, seats, fare);
            busDAO.generateSeats(busId, type, seats);
            fareCalc.addRoute(origin, dest, dist);
            db.commit();
            common.utilities.ConsoleUtils.printSuccess("Bus " + number + " added with " + seats + " seats.");
        } catch (Exception e) {
            db.rollback();
            common.utilities.ConsoleUtils.printError("Failed: " + e.getMessage());
        }
    }

    // ── Update bus status ─────────────────────────────────────────────────────
    public void updateBusStatus() throws common.exceptions.DatabaseException {
        String num = common.utilities.ConsoleUtils.readStringNonEmpty("Bus Number");
        bus.models.Bus b = busDAO.getByNumber(num);
        if (b == null) { common.utilities.ConsoleUtils.printError("Not found."); return; }
        String[] opts = {"ACTIVE","CANCELLED","MAINTENANCE"};
        int c = common.utilities.ConsoleUtils.showMenu("New Status", opts);
        if (c == 0) return;
        busDAO.updateStatus(b.getBusId(), opts[c-1]);
        common.utilities.ConsoleUtils.printSuccess("Updated to: " + opts[c-1]);
    }

    // ── View all bookings ─────────────────────────────────────────────────────
    public void viewAllBookings() throws common.exceptions.DatabaseException {
        String[] filters = {"All","CONFIRMED","CANCELLED"};
        int c = common.utilities.ConsoleUtils.showMenu("FILTER", filters);
        if (c == 0) return;
        String filter = c == 1 ? null : filters[c-1];
        var list = bookingDAO.getAllBookings(filter);
        common.utilities.ConsoleUtils.printHeader("ALL BUS BOOKINGS");
        if (list.isEmpty()) { common.utilities.ConsoleUtils.printInfo("None."); return; }
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

    // ── Occupancy report ──────────────────────────────────────────────────────
    public void occupancyReport() throws common.exceptions.DatabaseException {
        String date = common.utilities.ConsoleUtils.readStringNonEmpty("Date (YYYY-MM-DD)");
        common.utilities.ConsoleUtils.printHeader("BUS OCCUPANCY — " + date);
        var buses = busDAO.getAllBuses();
        String[] h = {"Bus","Type","Route","Total","Booked","Avail"};
        int[]    w = {18,12,22,6,7,6};
        common.utilities.ConsoleUtils.printTableHeader(h, w);
        for (int i = 0; i < buses.size(); i++) {
            bus.models.Bus b = buses.get(i);
            int booked = busDAO.countBookedSeats(b.getBusId(), date);
            int avail  = seatManager.getAvailableCount(b.getBusId(), date, b.getTotalSeats());
            common.utilities.ConsoleUtils.printTableRow(new String[]{
                b.getBusName(), b.getBusType(),
                b.getOrigin()+"→"+b.getDestination(),
                String.valueOf(b.getTotalSeats()),
                String.valueOf(booked), String.valueOf(avail)
            }, w);
        }
        common.utilities.ConsoleUtils.printTableSeparator(w);
    }

    // ── Seed sample data ──────────────────────────────────────────────────────
    public void seedSampleData() throws common.exceptions.DatabaseException {
        common.utilities.ConsoleUtils.printInfo("Seeding sample bus data...");
        common.database.DBConnection db = common.database.DBConnection.getInstance();
        try {
            db.beginTransaction();

            int r1 = busDAO.insertRoute("MUMBAI",    "PUNE",       150);
            int r2 = busDAO.insertRoute("DELHI",     "AGRA",       200);
            int r3 = busDAO.insertRoute("BANGALORE", "MYSORE",     140);
            int r4 = busDAO.insertRoute("AHMEDABAD", "SURAT",      260);
            int r5 = busDAO.insertRoute("CHENNAI",   "COIMBATORE", 490);
            int r6 = busDAO.insertRoute("JAIPUR",    "UDAIPUR",    395);

            int b1 = busDAO.insertBus("MH-01","Shivneri Express",r1,"AC_SEATER","06:00","09:00",44,2.50);
            busDAO.generateSeats(b1,"AC_SEATER",44);

            int b2 = busDAO.insertBus("DL-02","Agra Volvo",      r2,"AC_SEATER","07:30","11:00",40,2.20);
            busDAO.generateSeats(b2,"AC_SEATER",40);

            int b3 = busDAO.insertBus("KA-03","KSRTC Rajahamsa", r3,"AC_SEATER","08:00","11:00",44,2.00);
            busDAO.generateSeats(b3,"AC_SEATER",44);

            int b4 = busDAO.insertBus("GJ-04","Surat Sleeper",   r4,"AC_SLEEPER","21:00","02:00",36,1.80);
            busDAO.generateSeats(b4,"AC_SLEEPER",36);

            int b5 = busDAO.insertBus("TN-05","Coimbatore Night",r5,"SLEEPER","22:00","07:30",40,1.50);
            busDAO.generateSeats(b5,"SLEEPER",40);

            int b6 = busDAO.insertBus("RJ-06","Udaipur Express", r6,"SEATER","06:30","14:00",52,1.20);
            busDAO.generateSeats(b6,"SEATER",52);

            // Register routes in fare graph
            fareCalc.addRoute("MUMBAI","PUNE",150);
            fareCalc.addRoute("DELHI","AGRA",200);
            fareCalc.addRoute("BANGALORE","MYSORE",140);
            fareCalc.addRoute("AHMEDABAD","SURAT",260);
            fareCalc.addRoute("CHENNAI","COIMBATORE",490);
            fareCalc.addRoute("JAIPUR","UDAIPUR",395);

            db.commit();
            common.utilities.ConsoleUtils.printSuccess("Seeded 6 buses across 6 routes.");
        } catch (Exception e) {
            db.rollback();
            common.utilities.ConsoleUtils.printError("Seed failed: " + e.getMessage());
        }
    }
}
