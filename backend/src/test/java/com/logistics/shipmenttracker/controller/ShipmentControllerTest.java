package com.logistics.shipmenttracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.shipmenttracker.dto.ShipmentRequest;
import com.logistics.shipmenttracker.dto.ShipmentResponse;
import com.logistics.shipmenttracker.exception.DuplicateTrackingNumberException;
import com.logistics.shipmenttracker.exception.ShipmentNotFoundException;
import com.logistics.shipmenttracker.model.ShipmentStatus;
import com.logistics.shipmenttracker.service.ShipmentService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService service;

    private static final String VALID_BODY = """
            {
              "trackingNumber": "SHP-1001",
              "senderName": "North Warehouse",
              "receiverName": "City Pharmacy",
              "origin": "Leeds",
              "destination": "London",
              "carrier": "QuickMove Logistics",
              "status": "CREATED",
              "expectedDeliveryDate": "2026-09-25"
            }
            """;

    private ShipmentResponse sample() {
        return new ShipmentResponse(1L, "SHP-1001", "North Warehouse", "City Pharmacy", "Leeds", "London",
                "QuickMove Logistics", ShipmentStatus.CREATED, LocalDate.of(2026, 9, 25),
                Instant.parse("2026-09-19T10:30:00Z"), Instant.parse("2026-09-19T10:30:00Z"));
    }

    @Test
    void post_withValidInput_returns201AndLocation() throws Exception {
        when(service.create(any(ShipmentRequest.class))).thenReturn(sample());

        mockMvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/shipments/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.trackingNumber").value("SHP-1001"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-19T10:30:00Z"));
    }

    @Test
    void post_withInvalidInput_returns400WithFieldErrors() throws Exception {
        String invalid = """
                {"trackingNumber": "AB", "senderName": "", "receiverName": "City Pharmacy",
                 "origin": "Leeds", "destination": "London", "carrier": "QuickMove"}
                """;

        mockMvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/shipments"))
                .andExpect(jsonPath("$.fieldErrors.trackingNumber").value("Tracking number must be 5-30 characters"))
                .andExpect(jsonPath("$.fieldErrors.senderName").value("Sender name is required"))
                .andExpect(jsonPath("$.fieldErrors.expectedDeliveryDate").value("Expected delivery date is required"));
    }

    @Test
    void post_withInvalidStatus_returns400() throws Exception {
        String body = VALID_BODY.replace("\"CREATED\"", "\"TELEPORTED\"");

        mockMvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Allowed values")))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void post_withDuplicateTrackingNumber_returns409() throws Exception {
        when(service.create(any(ShipmentRequest.class))).thenThrow(new DuplicateTrackingNumberException("SHP-1001"));

        mockMvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.fieldErrors.trackingNumber").exists());
    }

    @Test
    void get_unknownId_returns404WithJsonError() throws Exception {
        when(service.findById(25L)).thenThrow(new ShipmentNotFoundException(25L));

        mockMvc.perform(get("/api/shipments/25"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Shipment with id 25 was not found"))
                .andExpect(jsonPath("$.path").value("/api/shipments/25"));
    }

    @Test
    void get_withNonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/shipments/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void list_withStatusFilter_passesStatusToService() throws Exception {
        when(service.findAll(eq(ShipmentStatus.IN_TRANSIT))).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/shipments").param("status", "IN_TRANSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void list_withInvalidStatusParam_returns400() throws Exception {
        mockMvc.perform(get("/api/shipments").param("status", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Allowed values")));
    }

    @Test
    void put_withValidInput_returns200() throws Exception {
        when(service.update(eq(1L), any(ShipmentRequest.class))).thenReturn(sample());

        mockMvc.perform(put("/api/shipments/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/shipments/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_unknownId_returns404() throws Exception {
        doThrow(new ShipmentNotFoundException(9L)).when(service).delete(9L);

        mockMvc.perform(delete("/api/shipments/9")).andExpect(status().isNotFound());
    }
}
