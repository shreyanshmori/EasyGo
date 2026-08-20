package common.exceptions;

public class DatabaseException extends ReservationException {
    public DatabaseException(String message, Throwable cause) {
        super("DB_ERROR", message, cause);
    }

    public DatabaseException(String message) {
        super("DB_ERROR", message);
    }
}
