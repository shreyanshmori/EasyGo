package common.exceptions;

public class TrainNotFoundException extends ReservationException {
    public TrainNotFoundException(String query) {
        super("TRAIN_NOT_FOUND", "No train found for: " + query);
    }
}
