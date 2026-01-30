package org.tc.mtracker.exceptions;

public class UserWithThisEmailAlreadyExistsException extends RuntimeException {
    public UserWithThisEmailAlreadyExistsException(String message) {
        super(message);
    }
}
