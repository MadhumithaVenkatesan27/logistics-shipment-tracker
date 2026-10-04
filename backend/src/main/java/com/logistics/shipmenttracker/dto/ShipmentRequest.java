package com.logistics.shipmenttracker.dto;

import com.logistics.shipmenttracker.model.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Request body for create (POST) and update (PUT). Text fields are trimmed before validation. */
public record ShipmentRequest(
        @NotBlank(message = "Tracking number is required")
        @Size(min = 5, max = 30, message = "Tracking number must be 5-30 characters")
        String trackingNumber,

        @NotBlank(message = "Sender name is required")
        @Size(min = 2, max = 100, message = "Sender name must be 2-100 characters")
        String senderName,

        @NotBlank(message = "Receiver name is required")
        @Size(min = 2, max = 100, message = "Receiver name must be 2-100 characters")
        String receiverName,

        @NotBlank(message = "Origin is required")
        @Size(min = 2, max = 100, message = "Origin must be 2-100 characters")
        String origin,

        @NotBlank(message = "Destination is required")
        @Size(min = 2, max = 100, message = "Destination must be 2-100 characters")
        String destination,

        @NotBlank(message = "Carrier is required")
        @Size(min = 2, max = 100, message = "Carrier must be 2-100 characters")
        String carrier,

        /** Optional on create (defaults to CREATED); required on update. */
        ShipmentStatus status,

        @NotNull(message = "Expected delivery date is required")
        LocalDate expectedDeliveryDate
) {
    public ShipmentRequest {
        trackingNumber = trim(trackingNumber);
        senderName = trim(senderName);
        receiverName = trim(receiverName);
        origin = trim(origin);
        destination = trim(destination);
        carrier = trim(carrier);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
