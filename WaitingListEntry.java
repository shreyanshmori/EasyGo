package railway.models;

public class WaitingListEntry {
    private int    wlId;
    private int    bookingId;
    private int    trainId;
    private int    coachId;
    private String journeyDate;
    private int    wlPosition;
    private String createdAt;
    private String passengerName;  // joined field

    public WaitingListEntry() {}
    public WaitingListEntry(int bookingId, int trainId, int coachId,
                             String journeyDate, int wlPosition) {
        this.bookingId   = bookingId;
        this.trainId     = trainId;
        this.coachId     = coachId;
        this.journeyDate = journeyDate;
        this.wlPosition  = wlPosition;
    }

    public int    getWlId()         { return wlId; }
    public int    getBookingId()    { return bookingId; }
    public int    getTrainId()      { return trainId; }
    public int    getCoachId()      { return coachId; }
    public String getJourneyDate()  { return journeyDate; }
    public int    getWlPosition()   { return wlPosition; }
    public String getCreatedAt()    { return createdAt; }
    public String getPassengerName(){ return passengerName; }

    public void setWlId(int id)             { this.wlId         = id; }
    public void setBookingId(int id)        { this.bookingId    = id; }
    public void setTrainId(int id)          { this.trainId      = id; }
    public void setCoachId(int id)          { this.coachId      = id; }
    public void setJourneyDate(String d)    { this.journeyDate  = d; }
    public void setWlPosition(int p)        { this.wlPosition   = p; }
    public void setCreatedAt(String t)      { this.createdAt    = t; }
    public void setPassengerName(String n)  { this.passengerName= n; }

    @Override
    public String toString() {
        return String.format("WL/%-3d | BookingID: %d | Date: %s", wlPosition, bookingId, journeyDate);
    }
}