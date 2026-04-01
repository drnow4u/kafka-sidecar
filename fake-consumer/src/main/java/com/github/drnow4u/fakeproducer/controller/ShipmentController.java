package com.github.drnow4u.fakeproducer.controller;

import com.github.drnow4u.fakeproducer.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private static final ConcurrentHashMap<String, Shipment> shipmentStore = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveShipment(@RequestBody Shipment shipment) {
        log.info("Received shipment: {}", shipment);

        if (!isValidShipment(shipment)) {
            log.warn("Invalid shipment received: {}", shipment.getShipmentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", "Invalid shipment data"));
        }

        shipmentStore.put(shipment.getShipmentId(), shipment);
        shipment.setStatus("IN_TRANSIT");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Shipment received and in transit");
        response.put("shipmentId", shipment.getShipmentId());
        response.put("orderId", shipment.getOrderId());
        response.put("trackingNumber", shipment.getTrackingNumber());
        response.put("carrier", shipment.getCarrier());
        response.put("estimatedDelivery", shipment.getEstimatedDelivery());
        response.put("timestamp", System.currentTimeMillis());

        log.info("Shipment {} processed successfully", shipment.getShipmentId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{shipmentId}")
    public ResponseEntity<Map<String, Object>> getShipment(@PathVariable String shipmentId) {
        Shipment shipment = shipmentStore.get(shipmentId);

        if (shipment == null) {
            log.warn("Shipment not found: {}", shipmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", "Shipment not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("shipment", shipment);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getShipmentByOrderId(@PathVariable String orderId) {
        Shipment shipment = shipmentStore.values().stream()
            .filter(s -> s.getOrderId().equals(orderId))
            .findFirst()
            .orElse(null);

        if (shipment == null) {
            log.warn("Shipment not found for order: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", "Shipment not found for order"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("shipment", shipment);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{shipmentId}/status")
    public ResponseEntity<Map<String, Object>> updateShipmentStatus(
            @PathVariable String shipmentId,
            @RequestBody Map<String, String> statusUpdate) {

        String newStatus = statusUpdate.get("status");

        if (newStatus == null || newStatus.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", "Status is required"));
        }

        Shipment shipment = shipmentStore.get(shipmentId);

        if (shipment == null) {
            log.warn("Shipment not found for status update: {}", shipmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", "Shipment not found"));
        }

        shipment.setStatus(newStatus);
        log.info("Shipment {} status updated to {}", shipmentId, newStatus);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Shipment status updated");
        response.put("shipmentId", shipmentId);
        response.put("newStatus", newStatus);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> getShipmentStats() {
        int totalShipments = shipmentStore.size();

        long inTransit = shipmentStore.values().stream()
            .filter(s -> "IN_TRANSIT".equals(s.getStatus()))
            .count();

        long delivered = shipmentStore.values().stream()
            .filter(s -> "DELIVERED".equals(s.getStatus()))
            .count();

        long pending = shipmentStore.values().stream()
            .filter(s -> "PENDING".equals(s.getStatus()))
            .count();

        log.info("Shipment Statistics - Total: {}, Pending: {}, In Transit: {}, Delivered: {}",
            totalShipments, pending, inTransit, delivered);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalShipments", totalShipments);
        response.put("statusBreakdown", Map.of(
            "PENDING", pending,
            "IN_TRANSIT", inTransit,
            "DELIVERED", delivered
        ));
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listAllShipments() {
        List<Shipment> shipments = shipmentStore.values().stream().toList();
        log.info("Retrieving all shipments. Total count: {}", shipments.size());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalShipments", shipments.size());
        response.put("shipments", shipments);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Shipment API",
            "timestamp", System.currentTimeMillis() + ""
        ));
    }

    private boolean isValidShipment(Shipment shipment) {
        if (shipment == null) {
            return false;
        }
        if (shipment.getShipmentId() == null || shipment.getShipmentId().isEmpty()) {
            log.warn("Shipment missing shipmentId");
            return false;
        }
        if (shipment.getOrderId() == null || shipment.getOrderId().isEmpty()) {
            log.warn("Shipment missing orderId");
            return false;
        }
        if (shipment.getTrackingNumber() == null || shipment.getTrackingNumber().isEmpty()) {
            log.warn("Shipment missing trackingNumber");
            return false;
        }
        return true;
    }
}

