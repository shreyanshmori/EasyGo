package common.payment;

import common.exceptions.DatabaseException;
import common.exceptions.PaymentFailedException;
import common.utilities.ConsoleUtils;
import datastructures.hashmap.CustomHashMap;

/**
 * Payment Verifier — Tracks and validates in-session payment attempts.
 *
 * Responsibilities:
 *   1. Rate limiting  — blocks after 3 failed attempts for a booking ref
 *   2. Duplicate check — prevents double-payment for same booking ref
 *   3. Amount integrity — confirms amount matches expected fare
 *   4. Session token store — maps txnId → status in a CustomHashMap
 *
 * Uses CustomHashMap (Phase 1) as the in-memory verification store.
 */
public class PaymentVerifier {

    // ── In-memory stores (session-scoped) ─────────────────────────────────────
    // bookingRef → attempt count
    private static final CustomHashMap<String, Integer> attemptMap  = new CustomHashMap<>();
    // txnId      → "SUCCESS" | "FAILED" | "REFUNDED"
    private static final CustomHashMap<String, String>  txnStore    = new CustomHashMap<>();
    // bookingRef → true if already paid
    private static final CustomHashMap<String, Boolean> paidMap     = new CustomHashMap<>();

    private static final int MAX_ATTEMPTS = 3;

    private final PaymentDAO paymentDAO;

    public PaymentVerifier() {
        this.paymentDAO = new PaymentDAO();
    }

    // ── Check if booking is already paid (prevents duplicate payment) ─────────
    public void checkNoDuplicate(String bookingRef)
            throws PaymentFailedException, DatabaseException {

        // Check in-memory first (same session)
        Boolean paid = paidMap.get(bookingRef);
        if (Boolean.TRUE.equals(paid))
            throw new PaymentFailedException("Booking " + bookingRef + " is already paid in this session.");

        // Check DB
        PaymentModel existing = paymentDAO.findByRef(bookingRef);
        if (existing != null && "SUCCESS".equals(existing.getPaymentStatus()))
            throw new PaymentFailedException("Booking " + bookingRef + " already has a successful payment.");
    }

    // ── Validate payment amount matches expected fare ─────────────────────────
    public void validateAmount(double expectedAmount, double providedAmount)
            throws PaymentFailedException {
        // Allow 1 rupee tolerance for rounding
        if (Math.abs(expectedAmount - providedAmount) > 1.0) {
            throw new PaymentFailedException(
                String.format("Amount mismatch. Expected: Rs. %.2f, Provided: Rs. %.2f",
                              expectedAmount, providedAmount));
        }
    }

    // ── Check attempt limit (rate limiting) ───────────────────────────────────
    public void checkAttemptLimit(String bookingRef) throws PaymentFailedException {
        int attempts = attemptMap.getOrDefault(bookingRef, 0);
        if (attempts >= MAX_ATTEMPTS) {
            throw new PaymentFailedException(
                "Maximum payment attempts (" + MAX_ATTEMPTS + ") exceeded for: " + bookingRef
                + ". Please contact support.");
        }
    }

    // ── Record a failed attempt ───────────────────────────────────────────────
    public void recordFailedAttempt(String bookingRef) {
        int current = attemptMap.getOrDefault(bookingRef, 0);
        attemptMap.put(bookingRef, current + 1);
        ConsoleUtils.printWarning("Failed attempt " + (current + 1) + "/" + MAX_ATTEMPTS
                + " for booking: " + bookingRef);
    }

    // ── Record a successful payment ───────────────────────────────────────────
    public void recordSuccess(String bookingRef, String txnId) {
        paidMap.put(bookingRef, true);
        txnStore.put(txnId, "SUCCESS");
        attemptMap.remove(bookingRef);   // reset attempts on success
    }

    // ── Verify a transaction ID exists and is successful ─────────────────────
    public boolean verifyTxnId(String txnId) throws DatabaseException {
        // Check session store first
        String status = txnStore.get(txnId);
        if ("SUCCESS".equals(status)) return true;

        // Check DB
        PaymentModel p = paymentDAO.findByTxnId(txnId);
        if (p != null && "SUCCESS".equals(p.getPaymentStatus())) {
            txnStore.put(txnId, "SUCCESS");   // cache in session
            return true;
        }
        return false;
    }

    // ── UPI UTR format validator ──────────────────────────────────────────────
    public static boolean isValidUTR(String utr) {
        if (utr == null || utr.trim().isEmpty()) return false;
        String u = utr.trim();
        // UTR: 12–22 alphanumeric characters
        if (u.length() < 12 || u.length() > 22) return false;
        for (int i = 0; i < u.length(); i++) {
            char c = u.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (!ok) return false;
        }
        return true;
    }

    // ── UPI amount validator ──────────────────────────────────────────────────
    public static boolean isValidUPIAmount(double amount) {
        // UPI limits: min Rs. 1, max Rs. 1,00,000 per transaction
        return amount >= 1.0 && amount <= 100000.0;
    }

    // ── Print verification status ─────────────────────────────────────────────
    public void printVerificationStatus(String bookingRef) {
        Boolean paid     = paidMap.get(bookingRef);
        Integer attempts = attemptMap.getOrDefault(bookingRef, 0);
        System.out.println();
        ConsoleUtils.printSubHeader("PAYMENT VERIFICATION STATUS");
        ConsoleUtils.printInfo("Booking Ref   : " + bookingRef);
        ConsoleUtils.printInfo("Paid          : " + (Boolean.TRUE.equals(paid) ? "YES" : "NO"));
        ConsoleUtils.printInfo("Failed Attempts: " + attempts + "/" + MAX_ATTEMPTS);
    }

    // ── Reset attempt counter (admin action) ─────────────────────────────────
    public void resetAttempts(String bookingRef) {
        attemptMap.remove(bookingRef);
        ConsoleUtils.printInfo("Attempt counter reset for: " + bookingRef);
    }

    // ── Clear entire session store (on logout) ────────────────────────────────
    public static void clearSession() {
        attemptMap.clear();
        txnStore.clear();
        paidMap.clear();
    }
}