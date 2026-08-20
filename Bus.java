package bus.models;

public class Bus {
    private int    busId;
    private String busNumber;
    private String busName;
    private int    routeId;
    private String busType;      // SEATER | SLEEPER | AC_SEATER | AC_SLEEPER
    private String departure;
    private String arrival;
    private int    totalSeats;
    private double farePerKm;
    private String status;       // ACTIVE | CANCELLED | MAINTENANCE

    // Joined fields
    private String origin;
    private String destination;
    private double distanceKm;

    public Bus() {}
    public Bus(int busId, String busNumber, String busName, int routeId,
               String busType, String departure, String arrival,
               int totalSeats, double farePerKm, String status) {
        this.busId      = busId;
        this.busNumber  = busNumber;
        this.busName    = busName;
        this.routeId    = routeId;
        this.busType    = busType;
        this.departure  = departure;
        this.arrival    = arrival;
        this.totalSeats = totalSeats;
        this.farePerKm  = farePerKm;
        this.status     = status;
    }

    public int    getBusId()      { return busId; }
    public String getBusNumber()  { return busNumber; }
    public String getBusName()    { return busName; }
    public int    getRouteId()    { return routeId; }
    public String getBusType()    { return busType; }
    public String getDeparture()  { return departure; }
    public String getArrival()    { return arrival; }
    public int    getTotalSeats() { return totalSeats; }
    public double getFarePerKm()  { return farePerKm; }
    public String getStatus()     { return status; }
    public String getOrigin()     { return origin; }
    public String getDestination(){ return destination; }
    public double getDistanceKm() { return distanceKm; }

    public void setBusId(int id)         { this.busId      = id; }
    public void setBusNumber(String n)   { this.busNumber  = n; }
    public void setBusName(String n)     { this.busName    = n; }
    public void setRouteId(int r)        { this.routeId    = r; }
    public void setBusType(String t)     { this.busType    = t; }
    public void setDeparture(String d)   { this.departure  = d; }
    public void setArrival(String a)     { this.arrival    = a; }
    public void setTotalSeats(int s)     { this.totalSeats = s; }
    public void setFarePerKm(double f)   { this.farePerKm  = f; }
    public void setStatus(String s)      { this.status     = s; }
    public void setOrigin(String o)      { this.origin     = o; }
    public void setDestination(String d) { this.destination= d; }
    public void setDistanceKm(double km) { this.distanceKm = km; }

    @Override
    public String toString() {
        return String.format("[%s] %-20s | %-12s | %s→%s | Dep:%-6s Arr:%-6s | Rs.%.2f/km",
            busNumber, busName, busType, origin, destination,
            departure, arrival, farePerKm);
    }
}


