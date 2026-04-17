package org.tc.mtracker.utils.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTransactionDateException extends ApiException {

    public InvalidTransactionDateException(String message) {
        super(HttpStatus.BAD_REQUEST, "invalid_transaction_date", message);
    }
}
