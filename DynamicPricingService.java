package flight.services;

import common.utilities.DateUtils;
import datastructures.heap.CustomHeap;
import datastructures.hashmap.CustomHashMap;

/**
 * DynamicPricingService — computes flight fares using demand-based rules.
 *
 * Pricing factors:
 *   1. Base fare      = distanceKm × baseFarePerKm (class-specific)
 *   2. Demand surge   = occupancy-based multiplier  (CustomHeap priority queue)
 *   3. Days-to-depart = last-minute premium / early-bird discount
 *   4. Peak season    = holiday/festival surcharge
 *   5. GST            = 5% domestic
 *
 * Uses:
 *   CustomHeap   — priority queue of fare tiers sorted by demand score
 *   CustomHashMap — fare cache per "flightId_classType_date"
 */
public class DynamicPricingService {

    // ── Base fare per km by class (Rs.) ───────────────────────────────────────
    private static final double BASE_ECONOMY  = 4.50;
    private static final double BASE_BUSINESS = 9.00;
    private static final double BASE_FIRST    = 14.00;
    private static final double GST_RATE      = 0.05;

    // ── Fare cache: "flightId_class_date" → computed fare ────────────────────
    private final CustomHashMap<String, Double> fareCache;

    // ── Demand tier heap (min-heap by available seats — fewer = higher price) ─
    private final CustomHeap<DemandTier> demandHeap;

    public DynamicPricingService() {
        this.fareCache   = new CustomHashMap<>();
        this.demandHeap  = new CustomHeap<>(16,
                (a, b) -> Integer.compare(a.availableSeats, b.availableSeats));
        initDemandTiers();
    }

    // ── Demand tiers ──────────────────────────────────────────────────────────
    private static class DemandTier implements Comparable<DemandTier> {
        int    availableSeats;   // upper bound of seats available for this tier
        double multiplier;       // fare multiplier

        DemandTier(int seats, double mult) {
            this.availableSeats = seats;
            this.multiplier     = mult;
        }

        @Override
        public int compareTo(DemandTier o) {
            return Integer.compare(this.availableSeats, o.availableSeats);
        }
    }

    private void initDemandTiers() {
        // seats available → price multiplier (fewer seats = more expensive)
        demandHeap.insert(new DemandTier(5,  2.50));   // nearly full  — 2.5×
        demandHeap.insert(new DemandTier(10, 2.00));   // very limited — 2.0×
        demandHeap.insert(new DemandTier(20, 1.60));   // limited      — 1.6×
        demandHeap.insert(new DemandTier(40, 1.30));   // moderate     — 1.3×
        demandHeap.insert(new DemandTier(60, 1.10));   // comfortable  — 1.1×
        demandHeap.insert(new DemandTier(999,1.00));   // plenty       — 1.0×
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN FARE CALCULATION
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Compute dynamic fare for one passenger.
     *
     * @param flightId      flight ID (for cache key)
     * @param distanceKm    route distance
     * @param classType     ECONOMY | BUSINESS | FIRST
     * @param journeyDate   YYYY-MM-DD
     * @param availSeats    remaining seats in class
     * @param passengers    number of passengers (for bulk discount)
     * @return              total fare (all passengers, incl. GST)
     */
    public double calculateFare(int flightId, double distanceKm,
                                String classType, String journeyDate,
                                int availSeats, int passengers) {

        String cacheKey = flightId + "_" + classType + "_" + journeyDate;
        Double cached   = fareCache.get(cacheKey);

        double perPax;
        if (cached != null) {
            perPax = cached;
        } else {
            double base      = baseFare(distanceKm, classType);
            double demand    = demandMultiplier(availSeats);
            double timeMult  = timeMultiplier(journeyDate);
            double seasonal  = seasonalMultiplier(journeyDate);
            double raw       = base * demand * timeMult * seasonal;
            perPax           = Math.round(raw * 100.0) / 100.0;
            fareCache.put(cacheKey, perPax);
        }

        // Bulk discount: 3+ passengers get 5% off per-pax
        double bulkDiscount = (passengers >= 3) ? 0.95 : 1.0;
        double subtotal     = perPax * passengers * bulkDiscount;
        double gst          = subtotal * GST_RATE;

        return Math.round((subtotal + gst) * 100.0) / 100.0;
    }

    /** Fare for a single passenger (no bulk discount, incl. GST). */
    public double farePerPassenger(int flightId, double distanceKm,
                                   String classType, String journeyDate,
                                   int availSeats) {
        return calculateFare(flightId, distanceKm, classType, journeyDate, availSeats, 1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FARE BREAKDOWN DISPLAY
    // ══════════════════════════════════════════════════════════════════════════

    public void printFareBreakdown(int flightId, double distanceKm,
                                   String classType, String journeyDate,
                                   int availSeats, int passengers) {
        double base     = baseFare(distanceKm, classType);
        double demand   = demandMultiplier(availSeats);
        double time     = timeMultiplier(journeyDate);
        double seasonal = seasonalMultiplier(journeyDate);
        double perPax   = Math.round(base * demand * time * seasonal * 100.0) / 100.0;
        double bulk     = (passengers >= 3) ? 0.95 : 1.0;
        double subtotal = perPax * passengers * bulk;
        double gst      = subtotal * GST_RATE;
        double total    = subtotal + gst;

        System.out.println();
        System.out.println("  ┌─── FARE BREAKDOWN ────────────────────────────────┐");
        System.out.printf ("  │  Class           : %-32s│%n", classType);
        System.out.printf ("  │  Distance        : %-6.0f km                       │%n", distanceKm);
        System.out.printf ("  │  Base Fare/Pax   : Rs. %-28.2f│%n", base);
        System.out.printf ("  │  Demand Factor   : %-32.2f│%n", demand);
        System.out.printf ("  │  Time Factor     : %-32.2f│%n", time);
        System.out.printf ("  │  Seasonal Factor : %-32.2f│%n", seasonal);
        System.out.printf ("  │  Fare / Pax      : Rs. %-28.2f│%n", perPax);
        System.out.printf ("  │  Passengers      : %-32d│%n", passengers);
        if (bulk < 1.0)
        System.out.printf ("  │  Bulk Discount   : 5%%%-30s│%n", "");
        System.out.printf ("  │  Subtotal        : Rs. %-28.2f│%n", subtotal);
        System.out.printf ("  │  GST (5%%)        : Rs. %-28.2f│%n", gst);
        System.out.println("  ├───────────────────────────────────────────────────┤");
        System.out.printf ("  │  TOTAL FARE      : Rs. %-28.2f│%n", total);
        System.out.println("  └───────────────────────────────────────────────────┘");
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRICING FACTORS
    // ══════════════════════════════════════════════════════════════════════════

    private double baseFare(double distanceKm, String classType) {
        double ratePerKm = switch (classType.toUpperCase()) {
            case "BUSINESS" -> BASE_BUSINESS;
            case "FIRST"    -> BASE_FIRST;
            default         -> BASE_ECONOMY;
        };
        return distanceKm * ratePerKm;
    }

    /** Demand multiplier — uses sorted DemandTier array (mirrors heap logic). */
    private double demandMultiplier(int availSeats) {
        // Linear scan through tiers (heap is for priority access pattern)
        int[]    thresholds   = {5,  10,  20,  40,  60,  999};
        double[] multipliers  = {2.50, 2.00, 1.60, 1.30, 1.10, 1.00};
        for (int i = 0; i < thresholds.length; i++) {
            if (availSeats <= thresholds[i]) return multipliers[i];
        }
        return 1.0;
    }

    /** Time-to-departure multiplier. */
    private double timeMultiplier(String journeyDate) {
        int days = DateUtils.daysBetween(DateUtils.today(), journeyDate);
        if (days <= 1)  return 2.20;   // last-minute — very expensive
        if (days <= 3)  return 1.80;
        if (days <= 7)  return 1.40;
        if (days <= 14) return 1.10;
        if (days <= 30) return 1.00;   // standard window
        if (days <= 60) return 0.90;   // early-bird discount
        return 0.80;                   // very early booking — cheapest
    }

    /** Seasonal / peak multiplier (month-based heuristic). */
    private double seasonalMultiplier(String journeyDate) {
        int month = Integer.parseInt(journeyDate.substring(5, 7));
        // Peak: Mar-Apr (summer), Oct-Nov (Diwali/festive), Dec (Christmas/New Year)
        return switch (month) {
            case 3, 4     -> 1.25;   // summer peak
            case 10, 11   -> 1.20;   // festive season
            case 12, 1    -> 1.15;   // winter holidays
            case 6, 7     -> 0.90;   // monsoon off-peak
            default       -> 1.00;
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CACHE MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    public void invalidateCache(int flightId, String classType, String date) {
        fareCache.remove(flightId + "_" + classType + "_" + date);
    }

    public void clearCache() {
        fareCache.clear();
    }
}