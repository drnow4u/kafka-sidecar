package com.github.drnow4u.kafkasidecar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private static final String SHIPMENT_TOPIC = "shipments";

    @Autowired
    private KafkaTemplate<String, Shipment> shipmentKafkaTemplate;

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveShipment(@RequestBody Shipment shipment) {
        log.info("Received shipment HTTP request: {}", shipment);

        if (!isValidShipment(shipment)) {
            log.warn("Invalid shipment received: {}", shipment.getShipmentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "Invalid shipment data"));
        }

        // Convert HTTP request to Kafka message
        publishShipmentToKafka(shipment);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Shipment received and published to Kafka");
        response.put("shipmentId", shipment.getShipmentId());
        response.put("orderId", shipment.getOrderId());
        response.put("trackingNumber", shipment.getTrackingNumber());
        response.put("carrier", shipment.getCarrier());
        response.put("estimatedDelivery", shipment.getEstimatedDelivery());
        response.put("topic", SHIPMENT_TOPIC);
        response.put("timestamp", System.currentTimeMillis());

        log.info("Shipment {} processed and published to Kafka topic: {}", shipment.getShipmentId(), SHIPMENT_TOPIC);
        return ResponseEntity.accepted().body(response);
    }

    private void publishShipmentToKafka(Shipment shipment) {
        try {
            Message<Shipment> message = MessageBuilder
                    .withPayload(shipment)
                    .setHeader(KafkaHeaders.TOPIC, SHIPMENT_TOPIC)
                    .setHeader("kafka_messageKey", shipment.getShipmentId())
                    .build();

            shipmentKafkaTemplate.send(message)
                    .thenAccept(result -> {
                        log.info("Successfully published shipment {} to Kafka topic: {}",
                                shipment.getShipmentId(), SHIPMENT_TOPIC);
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to publish shipment {} to Kafka: {}",
                                shipment.getShipmentId(), ex.getMessage(), ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error publishing shipment {} to Kafka: {}",
                    shipment.getShipmentId(), e.getMessage(), e);
        }
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

