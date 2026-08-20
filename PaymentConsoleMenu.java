package common.payment;

import common.exceptions.DatabaseException;
import common.utilities.ConsoleUtils;

/**
 * Standalone console menu for the Payment module.
 * Accessible from:
 *   - Main system menu (admin payment reports)
 *   - Each EasyGo cancellation flow
 *
 * This class is the UI layer for PaymentService.
 */
public class PaymentConsoleMenu {

    private final PaymentService service;

    public PaymentConsoleMenu() {
        this.service = new PaymentService();
    }

    // ── Main payment menu (admin) ─────────────────────────────────────────────
    public void showAdminMenu() {
        while (true) {
            String[] options = {
                "View All Payment History",
                "View Railway Payment History",
                "View Flight Payment History",
                "View Bus Payment History",
                "Revenue Report (All Systems)",
                "Verify a Transaction ID",
                "View Payment by Booking Reference",
                "View Cancellation Policy"
            };
            int choice = ConsoleUtils.showMenu("PAYMENT MANAGEMENT", options);
            if (choice == 0) break;

            try {
                switch (choice) {
                    case 1 -> service.showPaymentHistory(null);
                    case 2 -> service.showPaymentHistory("RAILWAY");
                    case 3 -> service.showPaymentHistory("FLIGHT");
                    case 4 -> service.showPaymentHistory("BUS");
                    case 5 -> service.showRevenueReport();
                    case 6 -> {
                        String txn = ConsoleUtils.readStringNonEmpty("Enter Transaction ID");
                        service.verifyTransaction(txn);
                    }
                    case 7 -> {
                        String ref = ConsoleUtils.readStringNonEmpty("Enter Booking Reference");
                        service.showBookingPaymentDetails(ref);
                    }
                    case 8 -> showPolicyMenu();
                }
            } catch (DatabaseException e) {
                ConsoleUtils.printError("Database error: " + e.getMessage());
            }

            ConsoleUtils.pause();
        }
    }

    // ── Policy submenu ────────────────────────────────────────────────────────
    private void showPolicyMenu() {
        String[] opts = {"Railway Policy", "Flight Policy", "Bus Policy"};
        int c = ConsoleUtils.showMenu("CANCELLATION POLICIES", opts);
        switch (c) {
            case 1 -> service.showCancellationPolicy("RAILWAY");
            case 2 -> service.showCancellationPolicy("FLIGHT");
            case 3 -> service.showCancellationPolicy("BUS");
        }
    }

    // ── Quick payment status check (used inline by booking flows) ────────────
    public void quickStatusCheck() {
        String ref = ConsoleUtils.readStringNonEmpty("Enter Booking Reference");
        try {
            String status = service.getPaymentStatus(ref);
            ConsoleUtils.printInfo("Payment Status for [" + ref + "]: " + status);
        } catch (DatabaseException e) {
            ConsoleUtils.printError("Could not fetch status: " + e.getMessage());
        }
        ConsoleUtils.pause();
    }
}
