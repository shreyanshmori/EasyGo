package flight.models;

public class FlightSeat {
    private int     seatId;
    private int     flightId;
    private String  seatNumber;
    private String  classType;   // ECONOMY | BUSINESS | FIRST
    private boolean isBooked;

    public FlightSeat() {}
    public FlightSeat(int seatId, int flightId, String seatNumber,
                      String classType, boolean isBooked) {
        this.seatId     = seatId;
        this.flightId   = flightId;
        this.seatNumber = seatNumber;
        this.classType  = classType;
        this.isBooked   = isBooked;
    }

    public int     getSeatId()     { return seatId; }
    public int     getFlightId()   { return flightId; }
    public String  getSeatNumber() { return seatNumber; }
    public String  getClassType()  { return classType; }
    public boolean isBooked()      { return isBooked; }

    public void setSeatId(int id)        { this.seatId     = id; }
    public void setFlightId(int id)      { this.flightId   = id; }
    public void setSeatNumber(String s)  { this.seatNumber = s; }
    public void setClassType(String c)   { this.classType  = c; }
    public void setBooked(boolean b)     { this.isBooked   = b; }

    @Override
    public String toString() {
        return seatNumber + "(" + classType + ")" + (isBooked ? "[X]" : "[A]");
    }
}


