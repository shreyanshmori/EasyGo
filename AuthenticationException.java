package common.exceptions;

public class AuthenticationException extends ReservationException {
    public AuthenticationException(String message) {
        super("AUTH_FAILED", message);
    }
}
