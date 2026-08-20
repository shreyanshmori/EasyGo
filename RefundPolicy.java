package common.payment;

import common.utilities.DateUtils;

/**
 * Centralized refund policy engine.
 * Each system (Railway, Flight, Bus) has its own tiered cancellation rules.
 *
 * Returns refund percentage (0–100) based on days remaining to journey.
 * Called by cancellation services before invoking PaymentGateway.processRefund().
 */
public class RefundPolicy {

    // ══════════════════════════════════════════════════════════════════════════
    //  RAILWAY REFUND POLICY  (IRCTC-inspired rules)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * @param journeyDate   YYYY-MM-DD
     * @param bookingStatus "CONFIRMED" | "WAITING" | "RAC"
     * @return refund percentage (0–100)
     */
    public static double railwayRefundPercent(String journeyDate, String bookingStatus) {
        int daysLeft = DateUtils.daysBetween(DateUtils.today(), journeyDate);

        // Waiting list — always full refund
        if ("WAITING".equalsIgnoreCase(bookingStatus)) return 100.0;

        // RAC — full refund if > 2 days, else 50%
        if ("RAC".equalsIgnoreCase(bookingStatus)) return daysLeft > 2 ? 100.0 : 50.0;

        // Confirmed ticket tiered refund
        if (daysLeft >= 30) return 100.0;   // > 30 days  → full refund
        if (daysLeft >= 10) return 75.0;    // 10–29 days → 75%
        if (daysLeft >= 4)  return 50.0;    // 4–9 days   → 50%
        if (daysLeft >= 2)  return 25.0;    // 2–3 days   → 25%
        return 0.0;                          // < 2 days   → no refund
    }

    /**
     * Human-readable refund policy description for Railway.
     */
    public static String railwayPolicyDescription() {
        return """
            Railway Cancellation & Refund Policy:
            ┌─────────────────────┬──────────────────┐
            │ Days Before Journey │ Refund %          │
            ├─────────────────────┼──────────────────┤
            │ 30+ days            │ 100% (Full)       │
            │ 10–29 days          │ 75%               │
            │ 4–9 days            │ 50%               │
            │ 2–3 days            │ 25%               │
            │ < 2 days            │ No Refund         │
            │ Waiting List        │ 100% always       │
            │ RAC (>2 days)       │ 100%              │
            │ RAC (≤2 days)       │ 50%               │
            └─────────────────────┴──────────────────┘
            """;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FLIGHT REFUND POLICY  (airline-style)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * @param journeyDate  YYYY-MM-DD
     * @param classType    "ECONOMY" | "BUSINESS" | "FIRST"
     * @return refund percentage
     */
    public static double flightRefundPercent(String journeyDate, String classType) {
        int daysLeft = DateUtils.daysBetween(DateUtils.today(), journeyDate);

        // First class — more generous refund
        if ("FIRST".equalsIgnoreCase(classType)) {
            if (daysLeft >= 14) return 90.0;
            if (daysLeft >= 7)  return 70.0;
            if (daysLeft >= 3)  return 40.0;
            return 0.0;
        }

        // Business class
        if ("BUSINESS".equalsIgnoreCase(classType)) {
            if (daysLeft >= 14) return 85.0;
            if (daysLeft >= 7)  return 60.0;
            if (daysLeft >= 3)  return 30.0;
            return 0.0;
        }

        // Economy class (most restrictive)
        if (daysLeft >= 14) return 75.0;
        if (daysLeft >= 7)  return 50.0;
        if (daysLeft >= 3)  return 20.0;
        return 0.0;
    }

    /**
     * Human-readable refund policy description for Flight.
     */
    public static String flightPolicyDescription() {
        return """
            Flight Cancellation & Refund Policy:
            ┌──────────────┬──────────┬──────────┬──────────┐
            │ Days Before  │ Economy  │ Business │ First    │
            ├──────────────┼──────────┼──────────┼──────────┤
            │ 14+ days     │ 75%      │ 85%      │ 90%      │
            │ 7–13 days    │ 50%      │ 60%      │ 70%      │
            │ 3–6 days     │ 20%      │ 30%      │ 40%      │
            │ < 3 days     │ No refund│ No refund│ No refund│
            └──────────────┴──────────┴──────────┴──────────┘
            """;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BUS REFUND POLICY
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * @param journeyDate  YYYY-MM-DD
     * @param busType      "AC_SLEEPER" | "AC_SEATER" | "SLEEPER" | "SEATER"
     * @return refund percentage
     */
    public static double busRefundPercent(String journeyDate, String busType) {
        int daysLeft = DateUtils.daysBetween(DateUtils.today(), journeyDate);

        // AC buses — slightly stricter
        boolean isAC = busType != null && busType.toUpperCase().startsWith("AC");

        if (daysLeft >= 7)  return isAC ? 85.0 : 90.0;
        if (daysLeft >= 3)  return isAC ? 60.0 : 70.0;
        if (daysLeft >= 1)  return isAC ? 30.0 : 40.0;
        return 0.0;
    }

    /**
     * Human-readable refund policy description for Bus.
     */
    public static String busPolicyDescription() {
        return """
            Bus Cancellation & Refund Policy:
            ┌─────────────────────┬─────────────┬─────────────┐
            │ Days Before Journey │ Non-AC Bus  │ AC Bus      │
            ├─────────────────────┼─────────────┼─────────────┤
            │ 7+ days             │ 90%         │ 85%         │
            │ 3–6 days            │ 70%         │ 60%         │
            │ 1–2 days            │ 40%         │ 30%         │
            │ Same day            │ No Refund   │ No Refund   │
            └─────────────────────┴─────────────┴─────────────┘
            """;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UTILITY — calculate actual refund amount
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Compute final refund amount given original fare and refund %.
     */
    public static double calculateRefundAmount(double originalFare, double refundPercent) {
        return Math.round((originalFare * refundPercent / 100.0) * 100.0) / 100.0;
    }

    /**
     * Print a summary of what the user will receive on cancellation.
     */
    public static void printRefundSummary(double originalFare, double refundPercent, String system) {
        double refundAmt = calculateRefundAmount(originalFare, refundPercent);
        double deduction = originalFare - refundAmt;

        System.out.println();
        System.out.println("  ┌─── CANCELLATION REFUND SUMMARY ──────────────┐");
        System.out.printf ("  │  System          : %-28s│%n", system);
        System.out.printf ("  │  Original Fare   : Rs. %-24.2f│%n", originalFare);
        System.out.printf ("  │  Refund %%        : %-28.1f│%n", refundPercent);
        System.out.printf ("  │  Deduction       : Rs. %-24.2f│%n", deduction);
        System.out.printf ("  │  You will receive: Rs. %-24.2f│%n", refundAmt);
        System.out.println("  └───────────────────────────────────────────────┘");
        System.out.println();
    }
}