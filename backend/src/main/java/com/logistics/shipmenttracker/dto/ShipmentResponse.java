package com.logistics.shipmenttracker.dto;

import com.logistics.shipmenttracker.model.ShipmentStatus;
import java.time.Instant;
import java.time.LocalDate;

public record ShipmentResponse(
        Long id,
        String trackingNumber,
        String senderName,
        String receiverName,
        String origin,
        String destination,
        String carrier,
        ShipmentStatus status,
        LocalDate expectedDeliveryDate,
        Instant createdAt,
        Instant updatedAt
) {}
