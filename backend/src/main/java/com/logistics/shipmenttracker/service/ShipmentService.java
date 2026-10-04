package com.logistics.shipmenttracker.service;

import com.logistics.shipmenttracker.dto.ShipmentRequest;
import com.logistics.shipmenttracker.dto.ShipmentResponse;
import com.logistics.shipmenttracker.exception.DuplicateTrackingNumberException;
import com.logistics.shipmenttracker.exception.InvalidRequestException;
import com.logistics.shipmenttracker.exception.ShipmentNotFoundException;
import com.logistics.shipmenttracker.model.Shipment;
import com.logistics.shipmenttracker.model.ShipmentStatus;
import com.logistics.shipmenttracker.repository.ShipmentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {

    private final ShipmentRepository repository;
    private final ShipmentMapper mapper;
    private final Clock clock;

    public ShipmentService(ShipmentRepository repository, ShipmentMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public ShipmentResponse create(ShipmentRequest request) {
        LocalDate today = LocalDate.now(clock);
        if (request.expectedDeliveryDate().isBefore(today)) {
            throw new InvalidRequestException("expectedDeliveryDate",
                    "Expected delivery date must not be earlier than the creation date");
        }
        if (repository.existsByTrackingNumber(request.trackingNumber())) {
            throw new DuplicateTrackingNumberException(request.trackingNumber());
        }
        Shipment saved = saveOrThrowDuplicate(mapper.toEntity(request));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> findAll(ShipmentStatus status) {
        List<Shipment> shipments = (status == null)
                ? repository.findAllByOrderByCreatedAtDescIdDesc()
                : repository.findByStatusOrderByCreatedAtDescIdDesc(status);
        return shipments.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShipmentResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public ShipmentResponse update(Long id, ShipmentRequest request) {
        Shipment shipment = getOrThrow(id);

        if (request.status() == null) {
            throw new InvalidRequestException("status", "Status is required");
        }
        LocalDate createdDate = shipment.getCreatedAt().atZone(clock.getZone()).toLocalDate();
        if (request.expectedDeliveryDate().isBefore(createdDate)) {
            throw new InvalidRequestException("expectedDeliveryDate",
                    "Expected delivery date must not be earlier than the creation date");
        }
        if (repository.existsByTrackingNumberAndIdNot(request.trackingNumber(), id)) {
            throw new DuplicateTrackingNumberException(request.trackingNumber());
        }

        mapper.applyRequest(request, shipment);
        return mapper.toResponse(saveOrThrowDuplicate(shipment));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Shipment getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ShipmentNotFoundException(id));
    }

    /**
     * The existence check above gives a friendly error in the normal case; the unique constraint
     * in the database is the real guarantee when two requests race each other.
     */
    private Shipment saveOrThrowDuplicate(Shipment shipment) {
        try {
            return repository.saveAndFlush(shipment);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateTrackingNumberException(shipment.getTrackingNumber());
        }
    }
}
