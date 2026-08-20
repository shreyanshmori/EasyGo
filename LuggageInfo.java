package flight.models;

public class LuggageInfo {
    private int    luggageId;
    private int    passengerId;
    private int    bookingId;
    private double weightKg;
    private double extraCharge;

    // Free allowance per class
    public static final double ECONOMY_FREE_KG  = 15.0;
    public static final double BUSINESS_FREE_KG = 25.0;
    public static final double FIRST_FREE_KG    = 35.0;
    public static final double CHARGE_PER_KG    = 350.0; // Rs per extra kg

    public LuggageInfo() {}
    public LuggageInfo(int passengerId, int bookingId, double weightKg, double extraCharge) {
        this.passengerId = passengerId;
        this.bookingId   = bookingId;
        this.weightKg    = weightKg;
        this.extraCharge = extraCharge;
    }

    public int    getLuggageId()   { return luggageId; }
    public int    getPassengerId() { return passengerId; }
    public int    getBookingId()   { return bookingId; }
    public double getWeightKg()    { return weightKg; }
    public double getExtraCharge() { return extraCharge; }

    public void setLuggageId(int id)      { this.luggageId   = id; }
    public void setPassengerId(int id)    { this.passengerId = id; }
    public void setBookingId(int id)      { this.bookingId   = id; }
    public void setWeightKg(double kg)    { this.weightKg    = kg; }
    public void setExtraCharge(double c)  { this.extraCharge = c; }

    /** Calculate extra luggage charge given class type. */
    public static double calculateExtra(double kg, String classType) {
        double free = switch (classType.toUpperCase()) {
            case "BUSINESS" -> BUSINESS_FREE_KG;
            case "FIRST"    -> FIRST_FREE_KG;
            default         -> ECONOMY_FREE_KG;
        };
        double extra = Math.max(0, kg - free);
        return Math.round(extra * CHARGE_PER_KG * 100.0) / 100.0;
    }
}
