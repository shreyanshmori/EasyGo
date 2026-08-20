package railway.models;

public class Coach {
    private int coachId;
    private int trainId;
    private String coachName;
    private String coachType;
    private int totalSeats;
    private double farePerKm;

    public Coach() {}

    public Coach(int coachId, int trainId, String coachName, String coachType,
                 int totalSeats, double farePerKm) {
        this.coachId = coachId;
        this.trainId = trainId;
        this.coachName = coachName;
        this.coachType = coachType;
        this.totalSeats = totalSeats;
        this.farePerKm = farePerKm;
    }

    public int getCoachId() { return coachId; }
    public int getTrainId() { return trainId; }
    public String getCoachName() { return coachName; }
    public String getCoachType() { return coachType; }
    public int getTotalSeats() { return totalSeats; }
    public double getFarePerKm() { return farePerKm; }

    public void setCoachId(int coachId) { this.coachId = coachId; }
    public void setTrainId(int trainId) { this.trainId = trainId; }
    public void setCoachName(String coachName) { this.coachName = coachName; }
    public void setCoachType(String coachType) { this.coachType = coachType; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public void setFarePerKm(double farePerKm) { this.farePerKm = farePerKm; }

    @Override
    public String toString() {
        return String.format("%s (%s) | Seats: %d | Rs.%.2f/km",
            coachName, coachType, totalSeats, farePerKm);
    }
}
