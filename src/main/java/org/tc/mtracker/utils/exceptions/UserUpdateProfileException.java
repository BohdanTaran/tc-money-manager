package org.tc.mtracker.utils.exceptions;

import org.springframework.http.HttpStatus;

public class UserUpdateProfileException extends ApiException {
    public UserUpdateProfileException(String message) {
        super(HttpStatus.BAD_REQUEST, "user_update_profile_failed", message);
    }
}
