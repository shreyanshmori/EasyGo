package railway.models;

public class Train {
    private int    trainId;
    private String trainNumber;
    private String trainName;
    private int    routeId;
    private String departure;
    private String arrival;
    private int    totalSeats;
    private String status;

    // Joined fields (not stored in DB directly)
    private String origin;
    private String destination;
    private double distanceKm;

    public Train() {}
    public Train(int trainId, String trainNumber, String trainName,
                 int routeId, String departure, String arrival,
                 int totalSeats, String status) {
        this.trainId     = trainId;
        this.trainNumber = trainNumber;
        this.trainName   = trainName;
        this.routeId     = routeId;
        this.departure   = departure;
        this.arrival     = arrival;
        this.totalSeats  = totalSeats;
        this.status      = status;
    }

    public int    getTrainId()      { return trainId; }
    public String getTrainNumber()  { return trainNumber; }
    public String getTrainName()    { return trainName; }
    public int    getRouteId()      { return routeId; }
    public String getDeparture()    { return departure; }
    public String getArrival()      { return arrival; }
    public int    getTotalSeats()   { return totalSeats; }
    public String getStatus()       { return status; }
    public String getOrigin()       { return origin; }
    public String getDestination()  { return destination; }
    public double getDistanceKm()   { return distanceKm; }

    public void setTrainId(int id)          { this.trainId     = id; }
    public void setTrainNumber(String n)    { this.trainNumber = n; }
    public void setTrainName(String n)      { this.trainName   = n; }
    public void setRouteId(int r)           { this.routeId     = r; }
    public void setDeparture(String d)      { this.departure   = d; }
    public void setArrival(String a)        { this.arrival     = a; }
    public void setTotalSeats(int s)        { this.totalSeats  = s; }
    public void setStatus(String s)         { this.status      = s; }
    public void setOrigin(String o)         { this.origin      = o; }
    public void setDestination(String d)    { this.destination = d; }
    public void setDistanceKm(double km)    { this.distanceKm  = km; }

    @Override
    public String toString() {
        return String.format("[%s] %-30s | %s → %s | Dep: %s | Arr: %s | Status: %s",
            trainNumber, trainName, origin, destination, departure, arrival, status);
    }
}
