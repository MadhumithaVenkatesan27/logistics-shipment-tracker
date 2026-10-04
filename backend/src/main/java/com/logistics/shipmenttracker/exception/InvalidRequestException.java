package com.logistics.shipmenttracker.exception;

/** Business-rule validation failure that is tied to one request field. */
public class InvalidRequestException extends RuntimeException {
    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
