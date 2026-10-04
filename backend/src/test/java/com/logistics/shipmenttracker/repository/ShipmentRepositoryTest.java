package com.logistics.shipmenttracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.shipmenttracker.model.Shipment;
import com.logistics.shipmenttracker.model.ShipmentStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class ShipmentRepositoryTest {

    @Autowired
    private ShipmentRepository repository;

    private Shipment shipment(String trackingNumber, ShipmentStatus status) {
        Shipment s = new Shipment();
        s.setTrackingNumber(trackingNumber);
        s.setSenderName("Sender");
        s.setReceiverName("Receiver");
        s.setOrigin("Leeds");
        s.setDestination("London");
        s.setCarrier("QuickMove");
        s.setStatus(status);
        s.setExpectedDeliveryDate(LocalDate.now().plusDays(3));
        return s;
    }

    @Test
    void findByStatus_returnsOnlyMatchingShipments() {
        repository.save(shipment("SHP-0001", ShipmentStatus.IN_TRANSIT));
        repository.save(shipment("SHP-0002", ShipmentStatus.DELIVERED));
        repository.save(shipment("SHP-0003", ShipmentStatus.IN_TRANSIT));

        List<Shipment> result = repository.findByStatusOrderByCreatedAtDescIdDesc(ShipmentStatus.IN_TRANSIT);

        assertThat(result).hasSize(2)
                .allMatch(s -> s.getStatus() == ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void findByStatus_returnsEmptyListWhenNothingMatches() {
        repository.save(shipment("SHP-0001", ShipmentStatus.CREATED));

        assertThat(repository.findByStatusOrderByCreatedAtDescIdDesc(ShipmentStatus.CANCELLED)).isEmpty();
    }

    @Test
    void save_setsCreatedAtAndUpdatedAtAutomatically() {
        Shipment saved = repository.saveAndFlush(shipment("SHP-0001", ShipmentStatus.CREATED));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void existsByTrackingNumberAndIdNot_ignoresTheShipmentItself() {
        Shipment saved = repository.saveAndFlush(shipment("SHP-0001", ShipmentStatus.CREATED));

        assertThat(repository.existsByTrackingNumber("SHP-0001")).isTrue();
        assertThat(repository.existsByTrackingNumberAndIdNot("SHP-0001", saved.getId())).isFalse();
        assertThat(repository.existsByTrackingNumberAndIdNot("SHP-0001", saved.getId() + 1)).isTrue();
    }

    @Test
    void duplicateTrackingNumber_isRejectedByUniqueConstraint() {
        repository.saveAndFlush(shipment("SHP-0001", ShipmentStatus.CREATED));

        assertThatThrownBy(() -> repository.saveAndFlush(shipment("SHP-0001", ShipmentStatus.CREATED)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
