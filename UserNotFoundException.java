package common.exceptions;

public class UserNotFoundException extends ReservationException {
    public UserNotFoundException(String username) {
        super("USER_NOT_FOUND", "User not found: " + username);
    }
}
