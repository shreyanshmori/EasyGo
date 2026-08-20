package railway.models;

public class RACEntry {
    private int    racId;
    private int    bookingId;
    private int    trainId;
    private int    coachId;
    private String journeyDate;
    private int    racPosition;
    private String seatNumber;
    private String createdAt;

    public RACEntry() {}
    public RACEntry(int bookingId, int trainId, int coachId,
                    String journeyDate, int racPosition, String seatNumber) {
        this.bookingId   = bookingId;
        this.trainId     = trainId;
        this.coachId     = coachId;
        this.journeyDate = journeyDate;
        this.racPosition = racPosition;
        this.seatNumber  = seatNumber;
    }

    public int    getRacId()        { return racId; }
    public int    getBookingId()    { return bookingId; }
    public int    getTrainId()      { return trainId; }
    public int    getCoachId()      { return coachId; }
    public String getJourneyDate()  { return journeyDate; }
    public int    getRacPosition()  { return racPosition; }
    public String getSeatNumber()   { return seatNumber; }
    public String getCreatedAt()    { return createdAt; }

    public void setRacId(int id)            { this.racId        = id; }
    public void setBookingId(int id)        { this.bookingId    = id; }
    public void setTrainId(int id)          { this.trainId      = id; }
    public void setCoachId(int id)          { this.coachId      = id; }
    public void setJourneyDate(String d)    { this.journeyDate  = d; }
    public void setRacPosition(int p)       { this.racPosition  = p; }
    public void setSeatNumber(String s)     { this.seatNumber   = s; }
    public void setCreatedAt(String t)      { this.createdAt    = t; }

    @Override
    public String toString() {
        return String.format("RAC/%-3d | Seat: %-6s | BookingID: %d | Date: %s",
            racPosition, seatNumber, bookingId, journeyDate);
    }
}