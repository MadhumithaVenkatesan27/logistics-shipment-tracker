package com.logistics.shipmenttracker.service;

import com.logistics.shipmenttracker.dto.ShipmentRequest;
import com.logistics.shipmenttracker.dto.ShipmentResponse;
import com.logistics.shipmenttracker.model.Shipment;
import com.logistics.shipmenttracker.model.ShipmentStatus;
import org.springframework.stereotype.Component;

/** Maps between DTOs and the persistence entity so the entity is never exposed by the API. */
@Component
public class ShipmentMapper {

    public Shipment toEntity(ShipmentRequest request) {
        Shipment shipment = new Shipment();
        applyRequest(request, shipment);
        if (shipment.getStatus() == null) {
            shipment.setStatus(ShipmentStatus.CREATED);
        }
        return shipment;
    }

    public void applyRequest(ShipmentRequest request, Shipment shipment) {
        shipment.setTrackingNumber(request.trackingNumber());
        shipment.setSenderName(request.senderName());
        shipment.setReceiverName(request.receiverName());
        shipment.setOrigin(request.origin());
        shipment.setDestination(request.destination());
        shipment.setCarrier(request.carrier());
        shipment.setStatus(request.status());
        shipment.setExpectedDeliveryDate(request.expectedDeliveryDate());
    }

    public ShipmentResponse toResponse(Shipment s) {
        return new ShipmentResponse(
                s.getId(), s.getTrackingNumber(), s.getSenderName(), s.getReceiverName(),
                s.getOrigin(), s.getDestination(), s.getCarrier(), s.getStatus(),
                s.getExpectedDeliveryDate(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
