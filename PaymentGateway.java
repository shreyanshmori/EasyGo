package common.payment;

import common.database.DBConnection;
import common.exceptions.PaymentFailedException;
import common.exceptions.DatabaseException;
import common.utilities.ConsoleUtils;
import common.utilities.AuthManager;

import java.sql.Connection;

/**
 * Central Payment Gateway — reusable by Railway, Flight, and Bus systems.
 *
 * Supported methods : CASH | UPI (with ASCII QR code)
 * Features          : verification, receipt generation,
 *                     refund processing, history tracking.
 *
 * Every payment is wrapped in a JDBC transaction so a booking
 * record and its payment record are always committed together.
 */
public class PaymentGateway {

    // ── UPI merchant config (would be configurable in production) ─────────────
    private static final String UPI_ID       = "easygo@ybl";
    private static final String MERCHANT_NAME = "EasyGo";

    private final PaymentDAO    paymentDAO;
    private final DBConnection  dbConn;

    public PaymentGateway() {
        this.paymentDAO = new PaymentDAO();
        this.dbConn     = DBConnection.getInstance();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Process a payment for any system.
     *
     * @param bookingRef  Unique booking reference
     * @param systemType  "RAILWAY" | "FLIGHT" | "BUS"
     * @param amount      Amount in INR
     * @return  PaymentModel on success
     * @throws  PaymentFailedException on user abort or failure
     */
    public PaymentModel processPayment(String bookingRef,
                                       String systemType,
                                       double amount)
            throws PaymentFailedException, DatabaseException {

        ConsoleUtils.printHeader("PAYMENT GATEWAY");
        ConsoleUtils.printInfo("Booking Ref : " + bookingRef);
        ConsoleUtils.printInfo("System      : " + systemType);
        ConsoleUtils.printInfo(String.format("Amount      : Rs. %.2f", amount));
        ConsoleUtils.printLine('-');

        // ── Choose payment method ─────────────────────────────────────────────
        String[] methods = {"Cash Payment", "UPI / QR Code Payment"};
        int choice = ConsoleUtils.showMenu("SELECT PAYMENT METHOD", methods);

        if (choice == 0) throw new PaymentFailedException("Payment cancelled by user.");

        String method = (choice == 1) ? "CASH" : "UPI";
        String txnId  = AuthManager.generateTxnId();

        // ── Execute chosen flow ───────────────────────────────────────────────
        boolean success;
        if (method.equals("CASH")) {
            success = processCash(amount);
        } else {
            success = processUPI(bookingRef, amount);
        }

        if (!success) {
            // Record failed attempt
            paymentDAO.insert(bookingRef, systemType, amount, method, "FAILED", txnId + "_FAIL");
            throw new PaymentFailedException("Payment verification failed. Please try again.");
        }

        // ── Persist successful payment ────────────────────────────────────────
        Connection conn = dbConn.getConnection();
        PaymentModel payment;
        try {
            dbConn.beginTransaction();

            int payId = paymentDAO.insert(bookingRef, systemType, amount, method, "SUCCESS", txnId);
            payment   = new PaymentModel(payId, bookingRef, systemType,
                                         amount, method, "SUCCESS", txnId,
                                         java.time.LocalDateTime.now().toString());

            dbConn.commit();
        } catch (Exception e) {
            dbConn.rollback();
            throw new PaymentFailedException("Payment persistence failed: " + e.getMessage(), e);
        }

        // ── Print receipt ─────────────────────────────────────────────────────
        printReceipt(payment);
        return payment;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REFUND
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Process a refund for a given booking reference.
     * Calculates refund amount based on cancellation policy.
     *
     * @param bookingRef  Booking reference
     * @param systemType  System that issued the booking
     * @param refundPct   Refund percentage (0–100)
     */
    public PaymentModel processRefund(String bookingRef,
                                      String systemType,
                                      double refundPct)
            throws PaymentFailedException, DatabaseException {

        // Fetch original payment
        PaymentModel original = paymentDAO.findByRef(bookingRef);
        if (original == null)
            throw new PaymentFailedException("No payment record found for: " + bookingRef);
        if (original.getPaymentStatus().equals("REFUNDED"))
            throw new PaymentFailedException("Refund already processed for: " + bookingRef);

        double refundAmount = original.getAmount() * (refundPct / 100.0);

        ConsoleUtils.printHeader("REFUND PROCESSING");
        ConsoleUtils.printInfo("Booking Ref    : " + bookingRef);
        ConsoleUtils.printInfo("Original Amount: Rs. " + String.format("%.2f", original.getAmount()));
        ConsoleUtils.printInfo("Refund %        : " + refundPct + "%");
        ConsoleUtils.printInfo(String.format("Refund Amount  : Rs. %.2f", refundAmount));
        ConsoleUtils.printLine('-');

        if (!ConsoleUtils.readYesNo("Confirm refund")) {
            throw new PaymentFailedException("Refund cancelled by user.");
        }

        // Update status to REFUNDED
        try {
            dbConn.beginTransaction();
            paymentDAO.updateStatus(original.getTransactionId(), "REFUNDED");

            // Insert a new refund record in payment_history
            String refundTxn = AuthManager.generateTxnId() + "_REF";
            paymentDAO.insert(bookingRef, systemType, -refundAmount,
                              original.getPaymentMethod(), "REFUNDED", refundTxn);
            dbConn.commit();
        } catch (Exception e) {
            dbConn.rollback();
            throw new PaymentFailedException("Refund persistence failed: " + e.getMessage(), e);
        }

        PaymentModel refund = new PaymentModel(-1, bookingRef, systemType,
                refundAmount, original.getPaymentMethod(), "REFUNDED",
                "REFUND-" + original.getTransactionId(),
                java.time.LocalDateTime.now().toString());

        ConsoleUtils.printSuccess(String.format(
            "Refund of Rs. %.2f processed to your %s account.", refundAmount, original.getPaymentMethod()));

        return refund;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PAYMENT HISTORY (console display)
    // ══════════════════════════════════════════════════════════════════════════

    public void showHistory(String systemType) throws DatabaseException {
        var list = (systemType == null)
                   ? paymentDAO.findAll()
                   : paymentDAO.findBySystem(systemType);

        ConsoleUtils.printHeader("PAYMENT HISTORY"
                + (systemType != null ? " — " + systemType : " — ALL SYSTEMS"));

        if (list.isEmpty()) {
            ConsoleUtils.printInfo("No payment records found.");
            return;
        }

        String[] headers = {"Ref", "System", "Amount", "Method", "Status", "Date"};
        int[]    widths  = {18, 9, 10, 7, 10, 20};
        ConsoleUtils.printTableHeader(headers, widths);

        for (int i = 0; i < list.size(); i++) {
            PaymentModel p = list.get(i);
            ConsoleUtils.printTableRow(new String[]{
                p.getBookingRef(),
                p.getSystemType(),
                String.format("%.2f", p.getAmount()),
                p.getPaymentMethod(),
                p.getPaymentStatus(),
                p.getPaidAt().length() > 19 ? p.getPaidAt().substring(0, 19) : p.getPaidAt()
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);

        // Summary
        if (systemType != null) {
            double rev     = paymentDAO.totalRevenue(systemType);
            double refunds = paymentDAO.totalRefunds(systemType);
            ConsoleUtils.printInfo(String.format("Total Revenue : Rs. %.2f", rev));
            ConsoleUtils.printInfo(String.format("Total Refunds : Rs. %.2f", refunds));
            ConsoleUtils.printInfo(String.format("Net Revenue   : Rs. %.2f", rev - refunds));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    // ── Cash payment flow ─────────────────────────────────────────────────────
    private boolean processCash(double amount) {
        ConsoleUtils.printSubHeader("CASH PAYMENT");
        System.out.println();
        ConsoleUtils.printInfo(String.format("Please collect Rs. %.2f from the customer.", amount));
        System.out.println();

        double tendered = ConsoleUtils.readDouble("Enter amount tendered by customer (Rs.)");
        if (tendered < amount) {
            ConsoleUtils.printError(String.format(
                "Insufficient amount. Required: Rs. %.2f, Tendered: Rs. %.2f", amount, tendered));
            return false;
        }

        double change = tendered - amount;
        if (change > 0) {
            ConsoleUtils.printInfo(String.format("Change to return: Rs. %.2f", change));
        }

        System.out.println();
        return ConsoleUtils.readYesNo("Confirm cash received and payment complete");
    }

    // ── UPI payment flow ──────────────────────────────────────────────────────
    private boolean processUPI(String bookingRef, double amount) {
        ConsoleUtils.printSubHeader("UPI PAYMENT");
        System.out.println();

        // Show ASCII QR code
        QRGenerator.printUPIQR(UPI_ID, MERCHANT_NAME, amount, bookingRef);

        ConsoleUtils.printInfo("1. Open any UPI app (PhonePe / GPay / Paytm / BHIM)");
        ConsoleUtils.printInfo("2. Scan the QR code above OR pay to: " + UPI_ID);
        ConsoleUtils.printInfo(String.format("3. Enter amount: Rs. %.2f", amount));
        ConsoleUtils.printInfo("4. Use reference: " + bookingRef);
        System.out.println();

        // Simulate verification with UTR entry
        String utr = ConsoleUtils.readStringNonEmpty("Enter UTR / Transaction ID from your UPI app");
        if (utr.length() < 6) {
            ConsoleUtils.printError("Invalid UTR number. Must be at least 6 characters.");
            return false;
        }

        // Simulate payment verification (in production: call UPI API)
        ConsoleUtils.printInfo("Verifying payment with UPI gateway...");
        simulateDelay(1200);

        boolean verified = simulateUPIVerification(utr, amount);
        if (!verified) {
            ConsoleUtils.printError("UPI payment verification failed. Check UTR and try again.");
            return false;
        }

        ConsoleUtils.printSuccess("UPI Payment verified! UTR: " + utr);
        return true;
    }

    // ── Simulate UPI verification (deterministic, for demo) ──────────────────
    private boolean simulateUPIVerification(String utr, double amount) {
        // Accept any UTR that is at least 6 chars (simulated success)
        // In production: call NPCI / bank API with UTR number
        return utr != null && utr.trim().length() >= 6 && amount > 0;
    }

    // ── Print formatted receipt to console ────────────────────────────────────
    private void printReceipt(PaymentModel p) {
        System.out.println();
        ConsoleUtils.printHeader("PAYMENT SUCCESSFUL");
        ConsoleUtils.printInfo("Booking Ref    : " + p.getBookingRef());
        ConsoleUtils.printInfo("System         : " + p.getSystemType());
        ConsoleUtils.printInfo(String.format("Amount Paid    : Rs. %.2f", p.getAmount()));
        ConsoleUtils.printInfo("Payment Method : " + p.getPaymentMethod());
        ConsoleUtils.printInfo("Transaction ID : " + p.getTransactionId());
        ConsoleUtils.printInfo("Date & Time    : " + p.getPaidAt());
        ConsoleUtils.printLine('=');
        ConsoleUtils.printInfo("Please keep this receipt for your records.");
        System.out.println();
    }

    // ── Simulate network/processing delay (ms) ────────────────────────────────
    private void simulateDelay(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}