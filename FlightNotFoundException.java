package common.exceptions;

public class FlightNotFoundException extends ReservationException {
    public FlightNotFoundException(String query) {
        super("FLIGHT_NOT_FOUND", "No flight found for: " + query);
    }
}
