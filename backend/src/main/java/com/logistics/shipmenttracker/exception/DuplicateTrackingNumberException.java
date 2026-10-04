package com.logistics.shipmenttracker.exception;

public class DuplicateTrackingNumberException extends RuntimeException {
    public DuplicateTrackingNumberException(String trackingNumber) {
        super("A shipment with tracking number '" + trackingNumber + "' already exists");
    }
}
