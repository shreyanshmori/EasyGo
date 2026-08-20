package common.exceptions;

public class SeatNotAvailableException extends ReservationException {
    public SeatNotAvailableException(String message) {
        super("SEAT_UNAVAILABLE", message);
    }
}
