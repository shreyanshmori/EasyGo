// ═══════════════════════════════════════════════════════════════════════════════
// FILE: common/exceptions/ReservationException.java  (Base)
// ═══════════════════════════════════════════════════════════════════════════════
package common.exceptions;

/**
 * Base checked exception for the entire EasyGo system.
 * All domain-specific exceptions extend this.
 */
public class ReservationException extends Exception {
    private final String errorCode;

    public ReservationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ReservationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }

    @Override
    public String toString() {
        return "[" + errorCode + "] " + getMessage();
    }
}
