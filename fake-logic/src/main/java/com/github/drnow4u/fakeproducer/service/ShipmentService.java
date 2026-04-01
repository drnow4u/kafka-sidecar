package com.github.drnow4u.fakeproducer.service;

import com.github.drnow4u.fakeproducer.model.Order;
import com.github.drnow4u.fakeproducer.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ShipmentService {

    private static final ConcurrentHashMap<String, Shipment> shipmentStore = new ConcurrentHashMap<>();
    private static final String[] CARRIERS = {"UPS", "FedEx", "USPS", "DHL", "Amazon Logistics"};
    private static final String[] SHIPPING_METHODS = {"STANDARD", "EXPRESS", "OVERNIGHT", "ECONOMY"};
    private final Random random = new Random();

    public Shipment createShipmentFromOrder(Order order) {
        log.info("Creating shipment for order: {}", order.getOrderId());

        String shipmentId = "SHIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String trackingNumber = generateTrackingNumber();
        String carrier = CARRIERS[random.nextInt(CARRIERS.length)];
        String shippingMethod = SHIPPING_METHODS[random.nextInt(SHIPPING_METHODS.length)];
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime estimatedDelivery = calculateEstimatedDelivery(shippingMethod);
        int itemCount = order.getItems().size();

        Shipment shipment = new Shipment(
            shipmentId,
            order.getOrderId(),
            order.getCustomerId(),
            trackingNumber,
            carrier,
            "PENDING",
            order.getShippingAddress(),
            createdAt,
            estimatedDelivery,
            itemCount,
            shippingMethod
        );

        shipmentStore.put(shipmentId, shipment);
        log.info("Shipment created: {} for order: {} - Carrier: {}, Tracking: {}",
            shipmentId, order.getOrderId(), carrier, trackingNumber);

        return shipment;
    }

    private String generateTrackingNumber() {
        long trackingNumber = 1000000000000L + random.nextLong() % 9000000000000L;
        return String.valueOf(trackingNumber);
    }

    private LocalDateTime calculateEstimatedDelivery(String shippingMethod) {
        LocalDateTime estimatedDelivery = LocalDateTime.now();
        return switch (shippingMethod) {
            case "OVERNIGHT" -> estimatedDelivery.plusDays(1);
            case "EXPRESS" -> estimatedDelivery.plusDays(2);
            case "STANDARD" -> estimatedDelivery.plusDays(5);
            case "ECONOMY" -> estimatedDelivery.plusDays(7);
            default -> estimatedDelivery.plusDays(5);
        };
    }

    public Shipment getShipment(String shipmentId) {
        return shipmentStore.get(shipmentId);
    }

    public Shipment updateShipmentStatus(String shipmentId, String status) {
        Shipment shipment = shipmentStore.get(shipmentId);
        if (shipment != null) {
            shipment.setStatus(status);
            log.info("Shipment {} status updated to: {}", shipmentId, status);
        }
        return shipment;
    }

    public static ConcurrentHashMap<String, Shipment> getShipmentStore() {
        return shipmentStore;
    }

    public int getTotalShipmentsCreated() {
        return shipmentStore.size();
    }
}

