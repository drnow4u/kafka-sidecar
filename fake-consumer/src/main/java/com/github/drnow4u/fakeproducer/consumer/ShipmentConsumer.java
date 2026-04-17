package com.github.drnow4u.fakeproducer.consumer;

import com.github.drnow4u.fakeproducer.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ShipmentConsumer {

    private final List<Shipment> processedShipments = new ArrayList<>();

    @KafkaListener(topics = "shipments", groupId = "fake-consumer-shipment-group")
    public void consumeShipment(Shipment shipment) {
        log.info("Consumed shipment from Kafka: {}", shipment);

        if (!isValidShipment(shipment)) {
            log.warn("Invalid shipment consumed: {}", shipment.getShipmentId());
            return;
        }

        // Process shipment
        processShipment(shipment);

        // Store shipment for tracking
        processedShipments.add(shipment);
        log.info("Shipment {} stored successfully. Total shipments processed: {}",
                shipment.getShipmentId(), processedShipments.size());
    }

    private void processShipment(Shipment shipment) {
        try {
            log.info("Processing shipment {} - Order: {}, Carrier: {}, Status: {}",
                    shipment.getShipmentId(), shipment.getOrderId(),
                    shipment.getCarrier(), shipment.getStatus());

            // Update shipment status to IN_TRANSIT
            shipment.setStatus("IN_TRANSIT");
            log.info("Shipment {} status updated to: IN_TRANSIT", shipment.getShipmentId());

            // Log delivery information
            log.info("Shipment Details - Tracking: {}, Carrier: {}, Estimated Delivery: {}, Address: {}",
                    shipment.getTrackingNumber(), shipment.getCarrier(),
                    shipment.getEstimatedDelivery(), shipment.getShippingAddress());

        } catch (Exception e) {
            log.error("Error processing shipment {}: {}",
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

    public List<Shipment> getProcessedShipments() {
        return new ArrayList<>(processedShipments);
    }

    public void clearProcessedShipments() {
        processedShipments.clear();
    }

    public int getTotalShipmentsProcessed() {
        return processedShipments.size();
    }
}
