package org.tc.mtracker.utils.exceptions;

public class UserAlreadyActivatedException extends RuntimeException {
    public UserAlreadyActivatedException(String message) {
        super(message);
    }
}
