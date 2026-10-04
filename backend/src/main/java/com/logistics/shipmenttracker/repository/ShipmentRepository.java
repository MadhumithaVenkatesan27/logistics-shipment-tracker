package com.logistics.shipmenttracker.repository;

import com.logistics.shipmenttracker.model.Shipment;
import com.logistics.shipmenttracker.model.ShipmentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    List<Shipment> findAllByOrderByCreatedAtDescIdDesc();

    List<Shipment> findByStatusOrderByCreatedAtDescIdDesc(ShipmentStatus status);

    boolean existsByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumberAndIdNot(String trackingNumber, Long id);
}
