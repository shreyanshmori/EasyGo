package flight.models;

public class MealOption {
    private int    mealId;
    private int    passengerId;
    private int    bookingId;
    private String mealType;   // VEG | NON_VEG | VEGAN | JAIN | DIABETIC
    private double mealCharge;

    public MealOption() {}
    public MealOption(int passengerId, int bookingId, String mealType, double mealCharge) {
        this.passengerId = passengerId;
        this.bookingId   = bookingId;
        this.mealType    = mealType;
        this.mealCharge  = mealCharge;
    }

    public int    getMealId()      { return mealId; }
    public int    getPassengerId() { return passengerId; }
    public int    getBookingId()   { return bookingId; }
    public String getMealType()    { return mealType; }
    public double getMealCharge()  { return mealCharge; }

    public void setMealId(int id)         { this.mealId      = id; }
    public void setPassengerId(int id)    { this.passengerId = id; }
    public void setBookingId(int id)      { this.bookingId   = id; }
    public void setMealType(String t)     { this.mealType    = t; }
    public void setMealCharge(double c)   { this.mealCharge  = c; }
}

