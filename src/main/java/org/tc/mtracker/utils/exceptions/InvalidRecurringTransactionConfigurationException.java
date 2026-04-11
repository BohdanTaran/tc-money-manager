package org.tc.mtracker.utils.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidRecurringTransactionConfigurationException extends ApiException {

    public InvalidRecurringTransactionConfigurationException(String message) {
        super(HttpStatus.BAD_REQUEST, "invalid_recurring_transaction_configuration", message);
    }
}
