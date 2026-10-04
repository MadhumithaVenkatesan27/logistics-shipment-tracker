package com.logistics.shipmenttracker.exception;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(Long id) {
        super("Shipment with id " + id + " was not found");
    }
}
