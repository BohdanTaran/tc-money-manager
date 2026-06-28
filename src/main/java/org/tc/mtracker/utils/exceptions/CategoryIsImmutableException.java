package org.tc.mtracker.utils.exceptions;

import org.springframework.http.HttpStatus;

public class CategoryIsImmutableException extends ApiException {
    public CategoryIsImmutableException(String message) {
        super(HttpStatus.CONFLICT, "category_is_immutable", message);
    }
}
