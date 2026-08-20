package common.database;

import bus.dao.BusDAO;
import common.exceptions.DatabaseException;
import flight.dao.FlightDAO;
import railway.dao.TrainDAO;

/**
 * Seeds default Indian routes (and sample trains / flights / buses) on first run
 * when a transport system has no routes yet.
 */
public final class DefaultIndiaDataSeeder {

    private DefaultIndiaDataSeeder() {}

    public static void seedIfEmpty() {
        try {
            TrainDAO  trainDAO  = new TrainDAO();
            FlightDAO flightDAO = new FlightDAO();
            BusDAO    busDAO    = new BusDAO();

            boolean seeded = false;
            if (trainDAO.getAllRoutes().isEmpty()) {
                seedRailway(trainDAO);
                seeded = true;
            }
            if (flightDAO.getAllRoutes().isEmpty()) {
                seedFlights(flightDAO);
                seeded = true;
            }
            if (busDAO.getAllRoutes().isEmpty()) {
                seedBuses(busDAO);
                seeded = true;
            }
            if (seeded) {
                System.out.println("[SYSTEM] Default India routes and services loaded.");
            }
        } catch (DatabaseException e) {
            System.err.println("[SYSTEM] Could not seed India data: " + e.getMessage());
        }
    }

    private static void seedRailway(TrainDAO dao) throws DatabaseException {
        DBConnection db = DBConnection.getInstance();
        try {
            db.beginTransaction();
            int r1  = dao.insertRoute("MUMBAI",      "NEW DELHI",   1384);
            int r2  = dao.insertRoute("NEW DELHI",   "KOLKATA",     1532);
            int r3  = dao.insertRoute("CHENNAI",     "BENGALURU",    362);
            int r4  = dao.insertRoute("MUMBAI",      "AHMEDABAD",    524);
            int r5  = dao.insertRoute("NEW DELHI",   "JAIPUR",       303);
            int r6  = dao.insertRoute("HYDERABAD",   "BENGALURU",    569);
            int r7  = dao.insertRoute("MUMBAI",      "PUNE",         192);
            int r8  = dao.insertRoute("NEW DELHI",   "CHANDIGARH",   244);
            int r9  = dao.insertRoute("KOCHI",       "THIRUVANANTHAPURAM", 220);
            int r10 = dao.insertRoute("VARANASI",    "LUCKNOW",      286);

            int t1 = dao.insertTrain("12951", "MUMBAI RAJDHANI",        r1, "16:25", "08:15", 500);
            dao.insertCoach(t1, "S1", "SL", 72, 0.45);
            dao.insertCoach(t1, "B1", "3A", 64, 0.85);
            dao.insertCoach(t1, "A1", "2A", 46, 1.20);
            dao.insertCoach(t1, "H1", "1A", 24, 1.80);

            int t2 = dao.insertTrain("12301", "KOLKATA RAJDHANI",       r2, "16:55", "10:00", 480);
            dao.insertCoach(t2, "S1", "SL", 72, 0.45);
            dao.insertCoach(t2, "B1", "3A", 64, 0.85);
            dao.insertCoach(t2, "A1", "2A", 46, 1.20);

            int t3 = dao.insertTrain("12007", "CHENNAI SHATABDI",       r3, "06:00", "11:00", 350);
            dao.insertCoach(t3, "C1", "CC", 78, 0.90);
            dao.insertCoach(t3, "C2", "CC", 78, 0.90);

            int t4 = dao.insertTrain("12901", "GUJARAT MAIL",           r4, "21:40", "07:10", 400);
            dao.insertCoach(t4, "S1", "SL", 72, 0.40);
            dao.insertCoach(t4, "B1", "3A", 64, 0.75);
            dao.insertCoach(t4, "G1", "GEN", 90, 0.20);

            int t5 = dao.insertTrain("12958", "JAIPUR DOUBLE DECKER",   r5, "06:05", "10:40", 280);
            dao.insertCoach(t5, "C1", "CC", 78, 0.80);
            dao.insertCoach(t5, "C2", "CC", 78, 0.80);

            int t6 = dao.insertTrain("12785", "KACHEGUDA EXPRESS",      r6, "22:30", "10:15", 420);
            dao.insertCoach(t6, "S1", "SL", 72, 0.42);
            dao.insertCoach(t6, "B1", "3A", 64, 0.80);

            int t7 = dao.insertTrain("12127", "INTERCITY EXPRESS",      r7, "06:40", "09:55", 300);
            dao.insertCoach(t7, "C1", "CC", 78, 0.75);
            dao.insertCoach(t7, "G1", "GEN", 90, 0.25);

            int t8 = dao.insertTrain("12011", "KALKA SHATABDI",         r8, "07:40", "11:45", 260);
            dao.insertCoach(t8, "C1", "CC", 78, 0.85);

            db.commit();
        } catch (Exception e) {
            try { db.rollback(); } catch (Exception ignored) { }
            throw new DatabaseException("Railway seed failed: " + e.getMessage(), e);
        }
    }

    private static void seedFlights(FlightDAO dao) throws DatabaseException {
        DBConnection db = DBConnection.getInstance();
        try {
            db.beginTransaction();
            int r1  = dao.insertRoute("DEL", "BOM", 1150);
            int r2  = dao.insertRoute("BOM", "DEL", 1150);
            int r3  = dao.insertRoute("DEL", "BLR", 1740);
            int r4  = dao.insertRoute("BOM", "BLR",  840);
            int r5  = dao.insertRoute("DEL", "HYD", 1250);
            int r6  = dao.insertRoute("DEL", "MAA", 1760);
            int r7  = dao.insertRoute("BOM", "GOI",  590);
            int r8  = dao.insertRoute("BLR", "HYD",  520);
            int r9  = dao.insertRoute("DEL", "AMD",  750);
            int r10 = dao.insertRoute("CCU", "DEL", 1300);

            int f1 = dao.insertFlight("AI-101",  "Air India",   r1,  "06:00", "08:10", 186);
            dao.generateSeats(f1, 150, 28, 8);

            int f2 = dao.insertFlight("6E-201",  "IndiGo",      r2,  "07:30", "09:45", 180);
            dao.generateSeats(f2, 160, 16, 4);

            int f3 = dao.insertFlight("AI-505",  "Air India",   r3,  "09:00", "12:30", 210);
            dao.generateSeats(f3, 170, 30, 10);

            int f4 = dao.insertFlight("SG-301",  "SpiceJet",    r4,  "11:00", "12:50", 168);
            dao.generateSeats(f4, 148, 16, 4);

            int f5 = dao.insertFlight("UK-201",  "Vistara",     r5,  "14:00", "16:15", 200);
            dao.generateSeats(f5, 160, 28, 12);

            int f6 = dao.insertFlight("6E-601",  "IndiGo",      r6,  "05:45", "08:30", 175);
            dao.generateSeats(f6, 155, 16, 4);

            int f7 = dao.insertFlight("G8-401",  "Go First",    r7,  "10:20", "11:35", 160);
            dao.generateSeats(f7, 140, 16, 4);

            int f8 = dao.insertFlight("AI-801",  "Air India",   r8,  "16:30", "17:50", 165);
            dao.generateSeats(f8, 150, 12, 3);

            db.commit();
        } catch (Exception e) {
            try { db.rollback(); } catch (Exception ignored) { }
            throw new DatabaseException("Flight seed failed: " + e.getMessage(), e);
        }
    }

    private static void seedBuses(BusDAO dao) throws DatabaseException {
        DBConnection db = DBConnection.getInstance();
        try {
            db.beginTransaction();
            int r1  = dao.insertRoute("MUMBAI",      "PUNE",           150);
            int r2  = dao.insertRoute("NEW DELHI",   "AGRA",           200);
            int r3  = dao.insertRoute("BENGALURU",   "MYSURU",         140);
            int r4  = dao.insertRoute("AHMEDABAD",   "SURAT",          260);
            int r5  = dao.insertRoute("CHENNAI",     "COIMBATORE",     490);
            int r6  = dao.insertRoute("JAIPUR",      "UDAIPUR",        395);
            int r7  = dao.insertRoute("HYDERABAD",   "VIJAYAWADA",     275);
            int r8  = dao.insertRoute("KOLKATA",     "DURGAPUR",       165);
            int r9  = dao.insertRoute("LUCKNOW",     "KANPUR",         100);
            int r10 = dao.insertRoute("NAGPUR",      "INDORE",         520);

            int b1 = dao.insertBus("MH-01", "Shivneri Express",    r1,  "AC_SEATER",  "06:00", "09:00", 44, 2.50);
            dao.generateSeats(b1, "AC_SEATER", 44);

            int b2 = dao.insertBus("DL-02", "Agra Volvo",          r2,  "AC_SEATER",  "07:30", "11:00", 40, 2.20);
            dao.generateSeats(b2, "AC_SEATER", 40);

            int b3 = dao.insertBus("KA-03", "KSRTC Rajahamsa",     r3,  "AC_SEATER",  "08:00", "11:00", 44, 2.00);
            dao.generateSeats(b3, "AC_SEATER", 44);

            int b4 = dao.insertBus("GJ-04", "Surat Sleeper",       r4,  "AC_SLEEPER", "21:00", "02:00", 36, 1.80);
            dao.generateSeats(b4, "AC_SLEEPER", 36);

            int b5 = dao.insertBus("TN-05", "Coimbatore Express",  r5,  "SLEEPER",    "22:00", "07:30", 40, 1.50);
            dao.generateSeats(b5, "SLEEPER", 40);

            int b6 = dao.insertBus("RJ-06", "Udaipur Express",     r6,  "SEATER",     "06:30", "14:00", 52, 1.20);
            dao.generateSeats(b6, "SEATER", 52);

            int b7 = dao.insertBus("TS-07", "Vijayawada AC",       r7,  "AC_SEATER",  "23:00", "05:30", 42, 2.10);
            dao.generateSeats(b7, "AC_SEATER", 42);

            int b8 = dao.insertBus("WB-08", "Durgapur Night",      r8,  "SLEEPER",    "20:30", "01:00", 38, 1.40);
            dao.generateSeats(b8, "SLEEPER", 38);

            db.commit();
        } catch (Exception e) {
            try { db.rollback(); } catch (Exception ignored) { }
            throw new DatabaseException("Bus seed failed: " + e.getMessage(), e);
        }
    }
}
