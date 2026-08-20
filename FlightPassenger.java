package flight.models;

public class FlightPassenger {
    private int    passengerId;
    private int    bookingId;
    private String name;
    private int    age;
    private String gender;
    private String seatNumber;
    private String passportNo;
    private String nationality;

    // Linked add-ons (not DB columns — fetched separately)
    private String mealType;
    private double luggageKg;

    public FlightPassenger() {}
    public FlightPassenger(int bookingId, String name, int age, String gender,
                           String seatNumber, String passportNo, String nationality) {
        this.bookingId   = bookingId;
        this.name        = name;
        this.age         = age;
        this.gender      = gender;
        this.seatNumber  = seatNumber;
        this.passportNo  = passportNo;
        this.nationality = nationality;
    }

    public int    getPassengerId() { return passengerId; }
    public int    getBookingId()   { return bookingId; }
    public String getName()        { return name; }
    public int    getAge()         { return age; }
    public String getGender()      { return gender; }
    public String getSeatNumber()  { return seatNumber; }
    public String getPassportNo()  { return passportNo; }
    public String getNationality() { return nationality; }
    public String getMealType()    { return mealType; }
    public double getLuggageKg()   { return luggageKg; }

    public void setPassengerId(int id)    { this.passengerId = id; }
    public void setBookingId(int id)      { this.bookingId   = id; }
    public void setName(String n)         { this.name        = n; }
    public void setAge(int a)             { this.age         = a; }
    public void setGender(String g)       { this.gender      = g; }
    public void setSeatNumber(String s)   { this.seatNumber  = s; }
    public void setPassportNo(String p)   { this.passportNo  = p; }
    public void setNationality(String n)  { this.nationality = n; }
    public void setMealType(String m)     { this.mealType    = m; }
    public void setLuggageKg(double kg)   { this.luggageKg   = kg; }

    @Override
    public String toString() {
        return String.format("%-24s | Age:%-3d | %s | Seat:%-5s | %s",
            name, age, gender, seatNumber, nationality);
    }
}


