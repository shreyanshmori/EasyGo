package common.exceptions;

public class WaitingListFullException extends ReservationException {
    public WaitingListFullException() {
        super("WL_FULL", "Waiting list is full. No more bookings accepted.");
    }
}
