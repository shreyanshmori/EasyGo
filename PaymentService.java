package common.payment;

import common.exceptions.DatabaseException;
import common.exceptions.PaymentFailedException;
import common.utilities.ConsoleUtils;
import common.utilities.AuthManager;
import datastructures.linkedlist.CustomLinkedList;

/**
 * PaymentService — High-level facade used by all EasyGo services.
 *
 * Orchestrates: PaymentVerifier → PaymentGateway → RefundPolicy
 *
 * Every booking flow calls:
 *   PaymentService.pay(bookingRef, systemType, amount)
 *
 * Every cancellation flow calls:
 *   PaymentService.refund(bookingRef, systemType, journeyDate, classOrStatus, busType)
 */
public class PaymentService {

    private final PaymentGateway  gateway;
    private final PaymentVerifier verifier;
    private final PaymentDAO      dao;

    public PaymentService() {
        this.gateway  = new PaymentGateway();
        this.verifier = new PaymentVerifier();
        this.dao      = new PaymentDAO();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PAY  — entry point for all booking payments
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Full payment flow with duplicate check, attempt limiting, and persistence.
     *
     * @param bookingRef  Unique booking reference / PNR
     * @param systemType  "RAILWAY" | "FLIGHT" | "BUS"
     * @param amount      Total fare
     * @return PaymentModel on success
     */
    public PaymentModel pay(String bookingRef, String systemType, double amount)
            throws PaymentFailedException, DatabaseException {

        // 1. Guard: already paid?
        verifier.checkNoDuplicate(bookingRef);

        // 2. Guard: too many failed attempts?
        verifier.checkAttemptLimit(bookingRef);

        // 3. Guard: amount sanity
        if (amount <= 0)
            throw new PaymentFailedException("Invalid payment amount: Rs. " + amount);

        PaymentModel result;
        try {
            // 4. Delegate to gateway (handles CASH / UPI + DB persistence)
            result = gateway.processPayment(bookingRef, systemType, amount);
        } catch (PaymentFailedException e) {
            // Record failed attempt and re-throw
            verifier.recordFailedAttempt(bookingRef);
            throw e;
        }

        // 5. Mark as paid in session cache
        verifier.recordSuccess(bookingRef, result.getTransactionId());

        // 6. Write to file-based receipt
        try {
            String file = common.filehandling.TicketFileWriter.generatePaymentReceipt(
                bookingRef, systemType, amount,
                result.getPaymentMethod(),
                result.getTransactionId(),
                result.getPaidAt()
            );
            ConsoleUtils.printInfo("Receipt saved: " + file);
        } catch (java.io.IOException e) {
            ConsoleUtils.printWarning("Receipt file could not be saved: " + e.getMessage());
        }

        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REFUND — entry point for all cancellation refunds
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Determines refund % via RefundPolicy, shows summary, then processes refund.
     *
     * @param bookingRef    Booking reference
     * @param systemType    "RAILWAY" | "FLIGHT" | "BUS"
     * @param journeyDate   YYYY-MM-DD (used for policy calculation)
     * @param qualifier     bookingStatus (Railway) | classType (Flight) | busType (Bus)
     */
    public PaymentModel refund(String bookingRef, String systemType,
                               String journeyDate, String qualifier)
            throws PaymentFailedException, DatabaseException {

        // 1. Determine refund percentage based on system type
        double refundPct;
        switch (systemType.toUpperCase()) {
            case "RAILWAY":
                refundPct = RefundPolicy.railwayRefundPercent(journeyDate, qualifier);
                break;
            case "FLIGHT":
                refundPct = RefundPolicy.flightRefundPercent(journeyDate, qualifier);
                break;
            case "BUS":
                refundPct = RefundPolicy.busRefundPercent(journeyDate, qualifier);
                break;
            default:
                throw new PaymentFailedException("Unknown system type: " + systemType);
        }

        // 2. Fetch original payment to get fare
        PaymentModel original = dao.findByRef(bookingRef);
        if (original == null)
            throw new PaymentFailedException("No payment record found for: " + bookingRef);

        // 3. Show refund summary to user
        RefundPolicy.printRefundSummary(original.getAmount(), refundPct, systemType);

        if (refundPct == 0.0) {
            ConsoleUtils.printWarning("No refund applicable as per cancellation policy.");
            // Still proceed with cancellation — just zero refund
        }

        // 4. Process via gateway
        return gateway.processRefund(bookingRef, systemType, refundPct);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HISTORY & REPORTS
    // ══════════════════════════════════════════════════════════════════════════

    /** Show payment history filtered by system type (null = all systems). */
    public void showPaymentHistory(String systemType) throws DatabaseException {
        gateway.showHistory(systemType);
    }

    /** Show full payment history for a specific booking reference. */
    public void showBookingPaymentDetails(String bookingRef) throws DatabaseException {
        PaymentModel p = dao.findByRef(bookingRef);
        if (p == null) {
            ConsoleUtils.printError("No payment record found for: " + bookingRef);
            return;
        }
        ConsoleUtils.printHeader("PAYMENT DETAILS — " + bookingRef);
        ConsoleUtils.printInfo("Payment ID     : " + p.getPaymentId());
        ConsoleUtils.printInfo("Booking Ref    : " + p.getBookingRef());
        ConsoleUtils.printInfo("System         : " + p.getSystemType());
        ConsoleUtils.printInfo(String.format("Amount         : Rs. %.2f", p.getAmount()));
        ConsoleUtils.printInfo("Method         : " + p.getPaymentMethod());
        ConsoleUtils.printInfo("Status         : " + p.getPaymentStatus());
        ConsoleUtils.printInfo("Transaction ID : " + p.getTransactionId());
        ConsoleUtils.printInfo("Paid At        : " + p.getPaidAt());
    }

    /** Revenue summary across all systems (admin report). */
    public void showRevenueReport() throws DatabaseException {
        ConsoleUtils.printHeader("REVENUE REPORT — ALL SYSTEMS");
        String[] systems = {"RAILWAY", "FLIGHT", "BUS"};
        double grandTotal = 0;
        double grandRefund = 0;

        String[] headers = {"System", "Revenue (Rs.)", "Refunds (Rs.)", "Net (Rs.)"};
        int[]    widths   = {12, 16, 16, 16};
        ConsoleUtils.printTableHeader(headers, widths);

        for (String sys : systems) {
            double rev     = dao.totalRevenue(sys);
            double refunds = dao.totalRefunds(sys);
            double net     = rev - refunds;
            grandTotal  += rev;
            grandRefund += refunds;
            ConsoleUtils.printTableRow(new String[]{
                sys,
                String.format("%.2f", rev),
                String.format("%.2f", refunds),
                String.format("%.2f", net)
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);
        ConsoleUtils.printTableRow(new String[]{
            "TOTAL",
            String.format("%.2f", grandTotal),
            String.format("%.2f", grandRefund),
            String.format("%.2f", grandTotal - grandRefund)
        }, widths);
        ConsoleUtils.printTableSeparator(widths);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  POLICY DISPLAY
    // ══════════════════════════════════════════════════════════════════════════

    /** Print cancellation policy for a given system. */
    public void showCancellationPolicy(String systemType) {
        ConsoleUtils.printHeader("CANCELLATION POLICY — " + systemType);
        switch (systemType.toUpperCase()) {
            case "RAILWAY" -> System.out.println(RefundPolicy.railwayPolicyDescription());
            case "FLIGHT"  -> System.out.println(RefundPolicy.flightPolicyDescription());
            case "BUS"     -> System.out.println(RefundPolicy.busPolicyDescription());
            default        -> ConsoleUtils.printError("Unknown system: " + systemType);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VERIFY
    // ══════════════════════════════════════════════════════════════════════════

    /** Verify if a transaction ID is valid and successful. */
    public boolean verifyTransaction(String txnId) throws DatabaseException {
        boolean valid = verifier.verifyTxnId(txnId);
        ConsoleUtils.printInfo("Transaction " + txnId + ": " + (valid ? "VERIFIED ✓" : "NOT FOUND ✗"));
        return valid;
    }

    /** Check payment status for a booking reference. */
    public String getPaymentStatus(String bookingRef) throws DatabaseException {
        PaymentModel p = dao.findByRef(bookingRef);
        return (p != null) ? p.getPaymentStatus() : "NOT_FOUND";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SESSION CLEANUP
    // ══════════════════════════════════════════════════════════════════════════

    /** Call on user logout to clear session-level caches. */
    public static void clearSession() {
        PaymentVerifier.clearSession();
    }
}