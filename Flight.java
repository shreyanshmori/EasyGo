package flight.models;

public class Flight {
    private int    flightId;
    private String flightNumber;
    private String airline;
    private int    routeId;
    private String departure;
    private String arrival;
    private int    totalSeats;
    private String status;      // SCHEDULED | CANCELLED | DELAYED | DEPARTED

    // Joined fields
    private String origin;
    private String destination;
    private double distanceKm;

    public Flight() {}
    public Flight(int flightId, String flightNumber, String airline, int routeId,
                  String departure, String arrival, int totalSeats, String status) {
        this.flightId     = flightId;
        this.flightNumber = flightNumber;
        this.airline      = airline;
        this.routeId      = routeId;
        this.departure    = departure;
        this.arrival      = arrival;
        this.totalSeats   = totalSeats;
        this.status       = status;
    }

    public int    getFlightId()     { return flightId; }
    public String getFlightNumber() { return flightNumber; }
    public String getAirline()      { return airline; }
    public int    getRouteId()      { return routeId; }
    public String getDeparture()    { return departure; }
    public String getArrival()      { return arrival; }
    public int    getTotalSeats()   { return totalSeats; }
    public String getStatus()       { return status; }
    public String getOrigin()       { return origin; }
    public String getDestination()  { return destination; }
    public double getDistanceKm()   { return distanceKm; }

    public void setFlightId(int id)       { this.flightId     = id; }
    public void setFlightNumber(String n) { this.flightNumber = n; }
    public void setAirline(String a)      { this.airline      = a; }
    public void setRouteId(int r)         { this.routeId      = r; }
    public void setDeparture(String d)    { this.departure    = d; }
    public void setArrival(String a)      { this.arrival      = a; }
    public void setTotalSeats(int s)      { this.totalSeats   = s; }
    public void setStatus(String s)       { this.status       = s; }
    public void setOrigin(String o)       { this.origin       = o; }
    public void setDestination(String d)  { this.destination  = d; }
    public void setDistanceKm(double km)  { this.distanceKm   = km; }

    @Override
    public String toString() {
        return String.format("[%s] %-10s | %-15s | %s→%s | Dep:%-6s Arr:%-6s | %s",
            flightNumber, airline, status, origin, destination, departure, arrival, status);
    }
}

