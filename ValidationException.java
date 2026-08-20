package common.exceptions;

public class ValidationException extends ReservationException {
    private final String fieldName;

    public ValidationException(String fieldName, String message) {
        super("VALIDATION_ERROR", "Field [" + fieldName + "]: " + message);
        this.fieldName = fieldName;
    }

    public String getFieldName() { return fieldName; }
}
