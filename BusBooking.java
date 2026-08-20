package bus.models;

public class BusBooking {
    private int    bookingId;
    private String bookingRef;
    private int    userId;
    private int    busId;
    private String journeyDate;
    private String bookingDate;
    private String status;       // CONFIRMED | CANCELLED | PENDING
    private double totalFare;

    // Joined display fields
    private String busNumber;
    private String busName;
    private String busType;
    private String origin;
    private String destination;
    private String departure;
    private String arrival;
    private double distanceKm;

    public BusBooking() {}

    public int    getBookingId()   { return bookingId; }
    public String getBookingRef()  { return bookingRef; }
    public int    getUserId()      { return userId; }
    public int    getBusId()       { return busId; }
    public String getJourneyDate() { return journeyDate; }
    public String getBookingDate() { return bookingDate; }
    public String getStatus()      { return status; }
    public double getTotalFare()   { return totalFare; }
    public String getBusNumber()   { return busNumber; }
    public String getBusName()     { return busName; }
    public String getBusType()     { return busType; }
    public String getOrigin()      { return origin; }
    public String getDestination() { return destination; }
    public String getDeparture()   { return departure; }
    public String getArrival()     { return arrival; }
    public double getDistanceKm()  { return distanceKm; }

    public void setBookingId(int id)      { this.bookingId   = id; }
    public void setBookingRef(String r)   { this.bookingRef  = r; }
    public void setUserId(int id)         { this.userId      = id; }
    public void setBusId(int id)          { this.busId       = id; }
    public void setJourneyDate(String d)  { this.journeyDate = d; }
    public void setBookingDate(String d)  { this.bookingDate = d; }
    public void setStatus(String s)       { this.status      = s; }
    public void setTotalFare(double f)    { this.totalFare   = f; }
    public void setBusNumber(String n)    { this.busNumber   = n; }
    public void setBusName(String n)      { this.busName     = n; }
    public void setBusType(String t)      { this.busType     = t; }
    public void setOrigin(String o)       { this.origin      = o; }
    public void setDestination(String d)  { this.destination = d; }
    public void setDeparture(String d)    { this.departure   = d; }
    public void setArrival(String a)      { this.arrival     = a; }
    public void setDistanceKm(double km)  { this.distanceKm  = km; }

    @Override
    public String toString() {
        return String.format("REF:%-14s | %s→%s | %s | %-10s | Rs.%.2f",
            bookingRef, origin, destination, journeyDate, status, totalFare);
    }
}


