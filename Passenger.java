package railway.models;

public class Passenger {
    private int    passengerId;
    private int    bookingId;
    private String name;
    private int    age;
    private String gender;      // M | F | O
    private String seatNumber;
    private String berthType;   // LB | MB | UB | SL | SU | WIN | RAC
    private String idType;
    private String idNumber;

    public Passenger() {}
    public Passenger(int bookingId, String name, int age, String gender,
                     String seatNumber, String berthType, String idType, String idNumber) {
        this.bookingId  = bookingId;
        this.name       = name;
        this.age        = age;
        this.gender     = gender;
        this.seatNumber = seatNumber;
        this.berthType  = berthType;
        this.idType     = idType;
        this.idNumber   = idNumber;
    }

    public int    getPassengerId() { return passengerId; }
    public int    getBookingId()   { return bookingId; }
    public String getName()        { return name; }
    public int    getAge()         { return age; }
    public String getGender()      { return gender; }
    public String getSeatNumber()  { return seatNumber; }
    public String getBerthType()   { return berthType; }
    public String getIdType()      { return idType; }
    public String getIdNumber()    { return idNumber; }

    public void setPassengerId(int id)      { this.passengerId = id; }
    public void setBookingId(int id)        { this.bookingId   = id; }
    public void setName(String n)           { this.name        = n; }
    public void setAge(int a)               { this.age         = a; }
    public void setGender(String g)         { this.gender      = g; }
    public void setSeatNumber(String s)     { this.seatNumber  = s; }
    public void setBerthType(String b)      { this.berthType   = b; }
    public void setIdType(String t)         { this.idType      = t; }
    public void setIdNumber(String n)       { this.idNumber    = n; }

    @Override
    public String toString() {
        return String.format("%-25s | Age: %3d | %s | Seat: %-6s | Berth: %-4s",
            name, age, gender, seatNumber, berthType);
    }
}

