package common.exceptions;

public class CancellationException extends ReservationException {
    public CancellationException(String message) {
        super("CANCELLATION_ERROR", message);
    }
}
