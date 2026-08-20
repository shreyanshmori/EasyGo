package common.exceptions;

public class InvalidPNRException extends ReservationException {
    public InvalidPNRException(String pnr) {
        super("INVALID_PNR", "Invalid or expired PNR: " + pnr);
    }
}
