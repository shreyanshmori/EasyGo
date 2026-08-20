package common.exceptions;

public class PaymentFailedException extends ReservationException {
    public PaymentFailedException(String message) {
        super("PAYMENT_FAILED", message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super("PAYMENT_FAILED", message, cause);
    }
}
