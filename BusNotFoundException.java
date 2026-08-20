package common.exceptions;

public class BusNotFoundException extends ReservationException {
    public BusNotFoundException(String query) {
        super("BUS_NOT_FOUND", "No bus found for: " + query);
    }
}
