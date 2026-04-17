package com.github.drnow4u.fakeproducer.controller;

import com.github.drnow4u.fakeproducer.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveShipment(@RequestBody Shipment shipment) {
        log.info("Received shipment: {}", shipment);

        if (!isValidShipment(shipment)) {
            log.warn("Invalid shipment received: {}", shipment.getShipmentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "error", "message", "Invalid shipment data"));
        }

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
        return ResponseEntity.accepted().body(response);
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
