package common.utilities;

import common.exceptions.DatabaseException;
import common.payment.PaymentConsoleMenu;
import common.payment.PaymentService;
import railway.services.RailwayAdminService;
import flight.services.FlightAdminService;
import bus.services.BusAdminService;

/**
 * AdminDashboard — master admin panel accessible from Main.java.
 *
 * Aggregates admin functions from all three systems plus:
 *   - System-wide stats
 *   - Cross-system payment report
 *   - User management
 *   - Login history
 *   - Database utilities
 */
public class AdminDashboard {

    private final AuthService        authService;
    private final RailwayAdminService railwayAdmin;
    private final FlightAdminService  flightAdmin;
    private final BusAdminService     busAdmin;
    private final PaymentConsoleMenu  paymentMenu;
    private final PaymentService      paymentService;

    public AdminDashboard() {
        this.authService    = new AuthService();
        this.railwayAdmin   = new RailwayAdminService();
        this.flightAdmin    = new FlightAdminService();
        this.busAdmin       = new BusAdminService();
        this.paymentMenu    = new PaymentConsoleMenu();
        this.paymentService = new PaymentService();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN ADMIN DASHBOARD MENU
    // ══════════════════════════════════════════════════════════════════════════

    public void show() {
        while (true) {
            printDashboardBanner();
            String[] opts = {
                "Railway Admin Panel",
                "Flight Admin Panel",
                "Bus Admin Panel",
                "Payment & Revenue Reports",
                "User Management",
                "Login History",
                "System Statistics",
                "Seed All Sample Data",
                "My Profile"
            };
            int choice = ConsoleUtils.showMenu("ADMIN DASHBOARD", opts);
            if (choice == 0) break;

            try {
                switch (choice) {
                    case 1 -> railwayAdminMenu();
                    case 2 -> flightAdminMenu();
                    case 3 -> busAdminMenu();
                    case 4 -> paymentMenu.showAdminMenu();
                    case 5 -> userManagementMenu();
                    case 6 -> authService.showLoginHistory();
                    case 7 -> showSystemStats();
                    case 8 -> seedAllData();
                    case 9 -> authService.showProfile();
                }
            } catch (Exception e) {
                ConsoleUtils.printError("Dashboard error: " + e.getMessage());
            }
            ConsoleUtils.pause();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SUB-MENUS (delegate to system admins)
    // ══════════════════════════════════════════════════════════════════════════

    private void railwayAdminMenu() throws DatabaseException {
        while (true) {
            String[] opts = {
                "Add New Train",
                "Update Train Status",
                "View All Bookings",
                "Seat Occupancy Report",
                "Monitor WL / RAC",
                "Seed Railway Data"
            };
            int c = ConsoleUtils.showMenu("RAILWAY ADMIN", opts);
            if (c == 0) break;
            try {
                switch (c) {
                    case 1 -> railwayAdmin.addTrainInteractive();
                    case 2 -> railwayAdmin.updateTrainStatus();
                    case 3 -> railwayAdmin.viewAllBookings();
                    case 4 -> railwayAdmin.seatOccupancyReport();
                    case 5 -> railwayAdmin.monitorWLRAC();
                    case 6 -> railwayAdmin.seedSampleData();
                }
            } catch (Exception e) {
                ConsoleUtils.printError(e.getMessage());
            }
            ConsoleUtils.pause();
        }
    }

    private void flightAdminMenu() throws DatabaseException {
        while (true) {
            String[] opts = {
                "Add New Flight",
                "Update Flight Status",
                "View All Bookings",
                "Occupancy Report",
                "Seed Flight Data"
            };
            int c = ConsoleUtils.showMenu("FLIGHT ADMIN", opts);
            if (c == 0) break;
            try {
                switch (c) {
                    case 1 -> flightAdmin.addFlightInteractive();
                    case 2 -> flightAdmin.updateFlightStatus();
                    case 3 -> flightAdmin.viewAllBookings();
                    case 4 -> flightAdmin.occupancyReport();
                    case 5 -> flightAdmin.seedSampleData();
                }
            } catch (Exception e) {
                ConsoleUtils.printError(e.getMessage());
            }
            ConsoleUtils.pause();
        }
    }

    private void busAdminMenu() throws DatabaseException {
        while (true) {
            String[] opts = {
                "Add New Bus",
                "Update Bus Status",
                "View All Bookings",
                "Occupancy Report",
                "Seed Bus Data"
            };
            int c = ConsoleUtils.showMenu("BUS ADMIN", opts);
            if (c == 0) break;
            try {
                switch (c) {
                    case 1 -> busAdmin.addBusInteractive();
                    case 2 -> busAdmin.updateBusStatus();
                    case 3 -> busAdmin.viewAllBookings();
                    case 4 -> busAdmin.occupancyReport();
                    case 5 -> busAdmin.seedSampleData();
                }
            } catch (Exception e) {
                ConsoleUtils.printError(e.getMessage());
            }
            ConsoleUtils.pause();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  USER MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    private void userManagementMenu() throws Exception {
        while (true) {
            String[] opts = {
                "View All Users",
                "Deactivate User",
                "Create New Admin"
            };
            int c = ConsoleUtils.showMenu("USER MANAGEMENT", opts);
            if (c == 0) break;
            switch (c) {
                case 1 -> authService.adminListUsers();
                case 2 -> authService.adminDeactivateUser();
                case 3 -> authService.adminCreateAdmin();
            }
            ConsoleUtils.pause();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SYSTEM STATISTICS
    // ══════════════════════════════════════════════════════════════════════════

    private void showSystemStats() throws DatabaseException {
        ConsoleUtils.printHeader("SYSTEM STATISTICS");
        paymentService.showRevenueReport();

        // Connection info
        ConsoleUtils.printLine('-');
        ConsoleUtils.printInfo("Database     : easygo.db");
        ConsoleUtils.printInfo("Admin User   : " + AuthManager.getUsername());
        ConsoleUtils.printInfo("Session Since: " + DateUtils.today());
        ConsoleUtils.printLine('-');
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEED ALL
    // ══════════════════════════════════════════════════════════════════════════

    private void seedAllData() {
        if (!ConsoleUtils.readYesNo("Seed sample data for ALL systems")) return;
        try {
            ConsoleUtils.printInfo("Seeding Railway...");
            railwayAdmin.seedSampleData();
            ConsoleUtils.printInfo("Seeding Flights...");
            flightAdmin.seedSampleData();
            ConsoleUtils.printInfo("Seeding Buses...");
            busAdmin.seedSampleData();
            ConsoleUtils.printSuccess("All sample data seeded successfully.");
        } catch (DatabaseException e) {
            ConsoleUtils.printError("Seed failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BANNER
    // ══════════════════════════════════════════════════════════════════════════

    private void printDashboardBanner() {
        ConsoleUtils.printLine('*');
        String line = "  ADMIN: " + AuthManager.getUsername()
                    + "   |   " + DateUtils.today();
        System.out.println(line);
        ConsoleUtils.printLine('*');
    }
}