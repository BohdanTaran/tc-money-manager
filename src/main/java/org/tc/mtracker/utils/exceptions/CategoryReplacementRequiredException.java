package org.tc.mtracker.utils.exceptions;

import org.springframework.http.HttpStatus;

public class CategoryReplacementRequiredException extends ApiException {
    public CategoryReplacementRequiredException(String message) {
        super(HttpStatus.BAD_REQUEST, "category_replacement_required", message);
    }
}
