package ca.truenorthtak.gateway.model;

/** Thrown when a message or encoded payload does not conform to its definition. */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) { super(message); }
    public ValidationException(String message, Throwable cause) { super(message, cause); }
}
