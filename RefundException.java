package common.exceptions;

public class RefundException extends ReservationException {
    public RefundException(String message) {
        super("REFUND_ERROR", message);
    }
}
