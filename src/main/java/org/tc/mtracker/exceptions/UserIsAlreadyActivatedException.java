package org.tc.mtracker.exceptions;

public class UserIsAlreadyActivatedException extends RuntimeException {
    public UserIsAlreadyActivatedException(String message) {
        super(message);
    }
}
