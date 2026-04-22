package org.tc.mtracker.utils.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidCategoryReplacementException extends ApiException {
    public InvalidCategoryReplacementException(String message) {
        super(HttpStatus.BAD_REQUEST, "invalid_category_replacement", message);
    }
}
