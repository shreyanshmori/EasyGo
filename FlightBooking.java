package flight.models;

public class FlightBooking {
    private int    bookingId;
    private String bookingRef;
    private int    userId;
    private int    flightId;
    private String journeyDate;
    private String classType;
    private String bookingDate;
    private String status;      // CONFIRMED | CANCELLED | PENDING
    private double totalFare;

    // Joined display fields
    private String flightNumber;
    private String airline;
    private String origin;
    private String destination;
    private String departure;
    private String arrival;

    public FlightBooking() {}

    public int    getBookingId()    { return bookingId; }
    public String getBookingRef()   { return bookingRef; }
    public int    getUserId()       { return userId; }
    public int    getFlightId()     { return flightId; }
    public String getJourneyDate()  { return journeyDate; }
    public String getClassType()    { return classType; }
    public String getBookingDate()  { return bookingDate; }
    public String getStatus()       { return status; }
    public double getTotalFare()    { return totalFare; }
    public String getFlightNumber() { return flightNumber; }
    public String getAirline()      { return airline; }
    public String getOrigin()       { return origin; }
    public String getDestination()  { return destination; }
    public String getDeparture()    { return departure; }
    public String getArrival()      { return arrival; }

    public void setBookingId(int id)       { this.bookingId   = id; }
    public void setBookingRef(String r)    { this.bookingRef  = r; }
    public void setUserId(int id)          { this.userId      = id; }
    public void setFlightId(int id)        { this.flightId    = id; }
    public void setJourneyDate(String d)   { this.journeyDate = d; }
    public void setClassType(String c)     { this.classType   = c; }
    public void setBookingDate(String d)   { this.bookingDate = d; }
    public void setStatus(String s)        { this.status      = s; }
    public void setTotalFare(double f)     { this.totalFare   = f; }
    public void setFlightNumber(String n)  { this.flightNumber= n; }
    public void setAirline(String a)       { this.airline     = a; }
    public void setOrigin(String o)        { this.origin      = o; }
    public void setDestination(String d)   { this.destination = d; }
    public void setDeparture(String d)     { this.departure   = d; }
    public void setArrival(String a)       { this.arrival     = a; }

    @Override
    public String toString() {
        return String.format("REF:%-14s | %s→%s | %s | %-10s | Rs.%.2f",
            bookingRef, origin, destination, journeyDate, status, totalFare);
    }
}


