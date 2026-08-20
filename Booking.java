package railway.models;

public class Booking {
    private int    bookingId;
    private String pnr;
    private int    userId;
    private int    trainId;
    private int    coachId;
    private String journeyDate;
    private String bookingDate;
    private String status;      // CONFIRMED | WAITING | RAC | CANCELLED
    private double totalFare;

    // Joined display fields
    private String trainNumber;
    private String trainName;
    private String coachName;
    private String coachType;
    private String origin;
    private String destination;
    private String departure;
    private String arrival;

    public Booking() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public int    getBookingId()    { return bookingId; }
    public String getPnr()          { return pnr; }
    public int    getUserId()       { return userId; }
    public int    getTrainId()      { return trainId; }
    public int    getCoachId()      { return coachId; }
    public String getJourneyDate()  { return journeyDate; }
    public String getBookingDate()  { return bookingDate; }
    public String getStatus()       { return status; }
    public double getTotalFare()    { return totalFare; }
    public String getTrainNumber()  { return trainNumber; }
    public String getTrainName()    { return trainName; }
    public String getCoachName()    { return coachName; }
    public String getCoachType()    { return coachType; }
    public String getOrigin()       { return origin; }
    public String getDestination()  { return destination; }
    public String getDeparture()    { return departure; }
    public String getArrival()      { return arrival; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setBookingId(int id)        { this.bookingId   = id; }
    public void setPnr(String pnr)          { this.pnr         = pnr; }
    public void setUserId(int id)           { this.userId      = id; }
    public void setTrainId(int id)          { this.trainId     = id; }
    public void setCoachId(int id)          { this.coachId     = id; }
    public void setJourneyDate(String d)    { this.journeyDate = d; }
    public void setBookingDate(String d)    { this.bookingDate = d; }
    public void setStatus(String s)         { this.status      = s; }
    public void setTotalFare(double f)      { this.totalFare   = f; }
    public void setTrainNumber(String n)    { this.trainNumber = n; }
    public void setTrainName(String n)      { this.trainName   = n; }
    public void setCoachName(String n)      { this.coachName   = n; }
    public void setCoachType(String t)      { this.coachType   = t; }
    public void setOrigin(String o)         { this.origin      = o; }
    public void setDestination(String d)    { this.destination = d; }
    public void setDeparture(String d)      { this.departure   = d; }
    public void setArrival(String a)        { this.arrival     = a; }

    @Override
    public String toString() {
        return String.format("PNR: %-15s | %s → %s | Date: %s | Status: %-10s | Fare: Rs.%.2f",
            pnr, origin, destination, journeyDate, status, totalFare);
    }
}

