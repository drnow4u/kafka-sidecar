package com.github.drnow4u.kafkasidecar;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private static final String SHIPMENT_TOPIC = "shipments";
    private static final String X_KAFKA_KEY = "x-kafka-key";

    @Autowired
    private KafkaTemplate<String, Shipment> shipmentKafkaTemplate;

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveShipment(
            @RequestBody Shipment shipment,
            @RequestHeader Map<String, String> httpHeaders) {
        log.info("Received shipment HTTP request: {} with headers: {}", shipment, httpHeaders);

        if (!isValidShipment(shipment)) {
            log.warn("Invalid shipment received: {}", shipment.getShipmentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "Invalid shipment data"));
        }

        // Convert HTTP request to Kafka message
        publishShipmentToKafka(shipment, httpHeaders);

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

    private void publishShipmentToKafka(Shipment shipment, Map<String, String> httpHeaders) {
        try {
            String key = httpHeaders.get(X_KAFKA_KEY);
            ProducerRecord<String, Shipment> producerRecord = new ProducerRecord<>(SHIPMENT_TOPIC, key, shipment);

            // Add all HTTP headers to Kafka message
            httpHeaders.forEach((headerKey, value) -> {
                if (value != null && !isReservedHeader(headerKey)) {
                    producerRecord.headers().add(new RecordHeader(headerKey, value.getBytes(StandardCharsets.UTF_8)));
                }
            });

            shipmentKafkaTemplate.send(producerRecord)
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

    private boolean isReservedHeader(String headerName) {
        String lowerHeader = headerName.toLowerCase();
        return lowerHeader.equals(HttpHeaders.CONTENT_TYPE.toLowerCase())
            || lowerHeader.equals(HttpHeaders.CONTENT_LENGTH.toLowerCase())
            || lowerHeader.equals(HttpHeaders.HOST.toLowerCase())
            || lowerHeader.equals(HttpHeaders.CONNECTION.toLowerCase())
            || lowerHeader.equals(X_KAFKA_KEY);
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

