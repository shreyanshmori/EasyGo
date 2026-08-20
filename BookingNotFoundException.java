package common.exceptions;

public class BookingNotFoundException extends ReservationException {
    public BookingNotFoundException(String ref) {
        super("BOOKING_NOT_FOUND", "Booking not found for reference: " + ref);
    }
}
