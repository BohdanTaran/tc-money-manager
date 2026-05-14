package org.tc.mtracker.utils.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidReceiptAttachmentException extends ApiException {

    public InvalidReceiptAttachmentException(String message) {
        super(HttpStatus.BAD_REQUEST, "invalid_receipt_attachment", message);
    }
}
