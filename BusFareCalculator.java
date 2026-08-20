package bus.services;

import common.utilities.ConsoleUtils;
import datastructures.graph.Graph;

/**
 * BusFareCalculator — distance-based fare engine using Graph shortest path.
 *
 * Graph represents the bus route network.
 * Dijkstra finds cheapest multi-hop path for indirect routes.
 *
 * Fare formula:
 *   base      = distanceKm × farePerKm
 *   type mult = SEATER 1.0 | AC_SEATER 1.3 | SLEEPER 1.1 | AC_SLEEPER 1.5
 *   gst       = 5%
 */
public class BusFareCalculator {

    // Route graph — vertices = city nodes, edges = route distances
    private final Graph routeGraph;
    private final datastructures.hashmap.CustomHashMap<String, Integer> cityIndex;
    private int vertexCount = 0;

    private static final int MAX_CITIES = 50;
    private static final double GST     = 0.05;

    public BusFareCalculator() {
        this.routeGraph = new Graph(MAX_CITIES);
        this.cityIndex  = new datastructures.hashmap.CustomHashMap<>();
    }

    // ── Register a city ───────────────────────────────────────────────────────
    public int registerCity(String city) {
        String key = city.toUpperCase();
        if (cityIndex.containsKey(key)) return cityIndex.get(key);
        int idx = vertexCount++;
        cityIndex.put(key, idx);
        routeGraph.setVertexName(idx, key);
        return idx;
    }

    // ── Add route to graph ────────────────────────────────────────────────────
    public void addRoute(String origin, String dest, double distanceKm) {
        int o = registerCity(origin);
        int d = registerCity(dest);
        routeGraph.addUndirectedEdge(o, d, distanceKm);
    }

    // ── Calculate fare (direct) ───────────────────────────────────────────────
    public double calculateFare(double distanceKm, double farePerKm, String busType) {
        double base     = distanceKm * farePerKm;
        double typeMult = typeMult(busType);
        double subtotal = base * typeMult;
        double gst      = subtotal * GST;
        return Math.round((subtotal + gst) * 100.0) / 100.0;
    }

    /** Calculate fare for multiple passengers with bulk discount. */
    public double calculateTotalFare(double distanceKm, double farePerKm,
                                     String busType, int passengers) {
        double perPax   = calculateFare(distanceKm, farePerKm, busType);
        double discount = (passengers >= 4) ? 0.95 : 1.0;   // 5% off for 4+
        return Math.round(perPax * passengers * discount * 100.0) / 100.0;
    }

    // ── Shortest path fare via graph ──────────────────────────────────────────
    public double shortestPathFare(String origin, String dest,
                                   double farePerKm, String busType) {
        Integer oIdx = cityIndex.get(origin.toUpperCase());
        Integer dIdx = cityIndex.get(dest.toUpperCase());
        if (oIdx == null || dIdx == null) return -1;

        double[] dist = routeGraph.dijkstra(oIdx);
        if (dist[dIdx] == Double.MAX_VALUE) return -1;
        return calculateFare(dist[dIdx], farePerKm, busType);
    }

    // ── Print route graph ─────────────────────────────────────────────────────
    public void printRouteNetwork() {
        ConsoleUtils.printHeader("BUS ROUTE NETWORK");
        routeGraph.printGraph();
    }

    // ── Print shortest path ───────────────────────────────────────────────────
    public void printShortestPath(String origin, String dest) {
        Integer oIdx = cityIndex.get(origin.toUpperCase());
        Integer dIdx = cityIndex.get(dest.toUpperCase());
        if (oIdx == null || dIdx == null) {
            ConsoleUtils.printError("City not found in route network.");
            return;
        }
        routeGraph.printShortestPath(oIdx, dIdx);
    }

    // ── Print fare breakdown ──────────────────────────────────────────────────
    public void printFareBreakdown(double distanceKm, double farePerKm,
                                   String busType, int passengers) {
        double base     = distanceKm * farePerKm;
        double typeMult = typeMult(busType);
        double subtotal = base * typeMult;
        double discount = (passengers >= 4) ? 0.95 : 1.0;
        double discounted = subtotal * passengers * discount;
        double gst      = discounted * GST;
        double total    = discounted + gst;

        System.out.println();
        System.out.println("  ┌─── FARE BREAKDOWN ─────────────────────────────────┐");
        System.out.printf ("  │  Bus Type        : %-32s│%n", busType);
        System.out.printf ("  │  Distance        : %-6.0f km                       │%n", distanceKm);
        System.out.printf ("  │  Fare / km       : Rs. %-28.2f│%n", farePerKm);
        System.out.printf ("  │  Base Fare       : Rs. %-28.2f│%n", base);
        System.out.printf ("  │  Type Multiplier : %-32.2f│%n", typeMult);
        System.out.printf ("  │  Passengers      : %-32d│%n", passengers);
        if (discount < 1.0)
        System.out.printf ("  │  Bulk Discount   : 5%%%-30s│%n", "");
        System.out.printf ("  │  Subtotal        : Rs. %-28.2f│%n", discounted);
        System.out.printf ("  │  GST (5%%)        : Rs. %-28.2f│%n", gst);
        System.out.println("  ├────────────────────────────────────────────────────┤");
        System.out.printf ("  │  TOTAL FARE      : Rs. %-28.2f│%n", total);
        System.out.println("  └────────────────────────────────────────────────────┘");
        System.out.println();
    }

    // ── Type multiplier ───────────────────────────────────────────────────────
    private double typeMult(String busType) {
        if (busType == null) return 1.0;
        return switch (busType.toUpperCase()) {
            case "AC_SLEEPER" -> 1.50;
            case "AC_SEATER"  -> 1.30;
            case "SLEEPER"    -> 1.10;
            default           -> 1.00;
        };
    }
}
