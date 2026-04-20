package org.tc.mtracker.utils.exceptions;

import org.springframework.http.HttpStatus;

public class RecurringTransactionNotFoundException extends ApiException {

    public RecurringTransactionNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "recurring_transaction_not_found", message);
    }
}
