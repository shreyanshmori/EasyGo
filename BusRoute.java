package bus.models;

public class BusRoute {
    private int    routeId;
    private String origin;
    private String destination;
    private double distanceKm;

    public BusRoute() {}
    public BusRoute(int routeId, String origin, String destination, double distanceKm) {
        this.routeId     = routeId;
        this.origin      = origin;
        this.destination = destination;
        this.distanceKm  = distanceKm;
    }

    public int    getRouteId()     { return routeId; }
    public String getOrigin()      { return origin; }
    public String getDestination() { return destination; }
    public double getDistanceKm()  { return distanceKm; }

    public void setRouteId(int id)       { this.routeId     = id; }
    public void setOrigin(String o)      { this.origin      = o; }
    public void setDestination(String d) { this.destination = d; }
    public void setDistanceKm(double km) { this.distanceKm  = km; }

    @Override
    public String toString() {
        return origin + " → " + destination + " (" + distanceKm + " km)";
    }
}
