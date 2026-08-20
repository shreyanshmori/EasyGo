package common.exceptions;

public class DuplicateBookingException extends ReservationException {
    public DuplicateBookingException(String ref) {
        super("DUPLICATE_BOOKING", "A booking already exists with reference: " + ref);
    }
}
