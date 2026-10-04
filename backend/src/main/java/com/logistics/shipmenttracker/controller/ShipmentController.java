package com.logistics.shipmenttracker.controller;

import com.logistics.shipmenttracker.dto.ShipmentRequest;
import com.logistics.shipmenttracker.dto.ShipmentResponse;
import com.logistics.shipmenttracker.model.ShipmentStatus;
import com.logistics.shipmenttracker.service.ShipmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> create(@Valid @RequestBody ShipmentRequest request) {
        ShipmentResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<ShipmentResponse> list(@RequestParam(required = false) ShipmentStatus status) {
        return service.findAll(status);
    }

    @GetMapping("/{id}")
    public ShipmentResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public ShipmentResponse update(@PathVariable Long id, @Valid @RequestBody ShipmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
