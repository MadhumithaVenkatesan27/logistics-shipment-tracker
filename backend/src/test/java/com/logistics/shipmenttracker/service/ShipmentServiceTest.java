package com.logistics.shipmenttracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.shipmenttracker.dto.ShipmentRequest;
import com.logistics.shipmenttracker.dto.ShipmentResponse;
import com.logistics.shipmenttracker.exception.DuplicateTrackingNumberException;
import com.logistics.shipmenttracker.exception.InvalidRequestException;
import com.logistics.shipmenttracker.exception.ShipmentNotFoundException;
import com.logistics.shipmenttracker.model.Shipment;
import com.logistics.shipmenttracker.model.ShipmentStatus;
import com.logistics.shipmenttracker.repository.ShipmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-19T10:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);

    @Mock
    private ShipmentRepository repository;

    private ShipmentService service;

    @BeforeEach
    void setUp() {
        service = new ShipmentService(repository, new ShipmentMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ShipmentRequest request(String trackingNumber, ShipmentStatus status, LocalDate date) {
        return new ShipmentRequest(trackingNumber, "North Warehouse", "City Pharmacy",
                "Leeds", "London", "QuickMove Logistics", status, date);
    }

    private Shipment existing(long id) {
        Shipment s = new Shipment();
        s.setId(id);
        s.setTrackingNumber("SHP-1001");
        s.setSenderName("North Warehouse");
        s.setReceiverName("City Pharmacy");
        s.setOrigin("Leeds");
        s.setDestination("London");
        s.setCarrier("QuickMove Logistics");
        s.setStatus(ShipmentStatus.CREATED);
        s.setExpectedDeliveryDate(TODAY.plusDays(5));
        s.setCreatedAt(NOW);
        s.setUpdatedAt(NOW);
        return s;
    }

    @Test
    void create_savesShipmentAndDefaultsStatusToCreated() {
        when(repository.existsByTrackingNumber("SHP-1001")).thenReturn(false);
        when(repository.saveAndFlush(any(Shipment.class))).thenAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            s.setId(1L);
            s.setCreatedAt(NOW);
            s.setUpdatedAt(NOW);
            return s;
        });

        ShipmentResponse response = service.create(request("SHP-1001", null, TODAY.plusDays(3)));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.trackingNumber()).isEqualTo("SHP-1001");
        assertThat(response.status()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void create_rejectsDuplicateTrackingNumber() {
        when(repository.existsByTrackingNumber("SHP-1001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("SHP-1001", ShipmentStatus.CREATED, TODAY)))
                .isInstanceOf(DuplicateTrackingNumberException.class)
                .hasMessageContaining("SHP-1001");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_translatesDatabaseUniqueViolationIntoDuplicateError() {
        when(repository.existsByTrackingNumber("SHP-1001")).thenReturn(false);
        when(repository.saveAndFlush(any(Shipment.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.create(request("SHP-1001", ShipmentStatus.CREATED, TODAY)))
                .isInstanceOf(DuplicateTrackingNumberException.class);
    }

    @Test
    void create_rejectsDeliveryDateBeforeCreationDate() {
        assertThatThrownBy(() -> service.create(request("SHP-1001", null, TODAY.minusDays(1))))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("field").isEqualTo("expectedDeliveryDate");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void findById_throwsWhenShipmentDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ShipmentNotFoundException.class)
                .hasMessage("Shipment with id 99 was not found");
    }

    @Test
    void findAll_withStatusUsesFilteredQuery() {
        Shipment s = existing(1L);
        s.setStatus(ShipmentStatus.IN_TRANSIT);
        when(repository.findByStatusOrderByCreatedAtDescIdDesc(ShipmentStatus.IN_TRANSIT)).thenReturn(List.of(s));

        List<ShipmentResponse> result = service.findAll(ShipmentStatus.IN_TRANSIT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        verify(repository, never()).findAllByOrderByCreatedAtDescIdDesc();
    }

    @Test
    void update_changesFieldsOfExistingShipment() {
        Shipment s = existing(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(s));
        when(repository.existsByTrackingNumberAndIdNot("SHP-1001", 1L)).thenReturn(false);
        when(repository.saveAndFlush(s)).thenReturn(s);

        ShipmentResponse response = service.update(1L,
                request("SHP-1001", ShipmentStatus.DELIVERED, TODAY.plusDays(2)));

        assertThat(response.status()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(response.expectedDeliveryDate()).isEqualTo(TODAY.plusDays(2));
    }

    @Test
    void update_throwsWhenShipmentDoesNotExist() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(5L, request("SHP-1001", ShipmentStatus.CREATED, TODAY)))
                .isInstanceOf(ShipmentNotFoundException.class);
    }

    @Test
    void update_rejectsTrackingNumberUsedByAnotherShipment() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing(1L)));
        when(repository.existsByTrackingNumberAndIdNot("SHP-2000", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("SHP-2000", ShipmentStatus.CREATED, TODAY)))
                .isInstanceOf(DuplicateTrackingNumberException.class);
    }

    @Test
    void delete_removesExistingShipment() {
        Shipment s = existing(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(s));

        service.delete(1L);

        verify(repository).delete(s);
    }

    @Test
    void delete_throwsWhenShipmentDoesNotExist() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(ShipmentNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
