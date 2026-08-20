package bus.models;

public class BusSeat {
    private int     seatId;
    private int     busId;
    private String  seatNumber;
    private String  seatType;    // WINDOW | AISLE | LOWER | UPPER
    private boolean isBooked;

    public BusSeat() {}
    public BusSeat(int seatId, int busId, String seatNumber,
                   String seatType, boolean isBooked) {
        this.seatId     = seatId;
        this.busId      = busId;
        this.seatNumber = seatNumber;
        this.seatType   = seatType;
        this.isBooked   = isBooked;
    }

    public int     getSeatId()     { return seatId; }
    public int     getBusId()      { return busId; }
    public String  getSeatNumber() { return seatNumber; }
    public String  getSeatType()   { return seatType; }
    public boolean isBooked()      { return isBooked; }

    public void setSeatId(int id)       { this.seatId     = id; }
    public void setBusId(int id)        { this.busId      = id; }
    public void setSeatNumber(String s) { this.seatNumber = s; }
    public void setSeatType(String t)   { this.seatType   = t; }
    public void setBooked(boolean b)    { this.isBooked   = b; }

    @Override
    public String toString() {
        return seatNumber + "[" + seatType.charAt(0) + "]" + (isBooked ? "X" : "A");
    }
}
