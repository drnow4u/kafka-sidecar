package com.github.drnow4u.fakeproducer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${shipment.endpoint:http://localhost:8081/api/shipments}")
    private String shipmentEndpoint;

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleOrder(@RequestBody Order order) {
        log.info("Received order: {}", order);

        // Create shipment from order
        Shipment shipment = shipmentService.createShipmentFromOrder(order);

        // Forward shipment to shipment service
        forwardShipmentToEndpoint(shipment);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Order received and shipment created");
        response.put("orderId", order.getOrderId());
        response.put("customerId", order.getCustomerId());
        response.put("totalAmount", order.getTotalAmount());
        response.put("shipmentId", shipment.getShipmentId());
        response.put("trackingNumber", shipment.getTrackingNumber());
        response.put("carrier", shipment.getCarrier());
        response.put("estimatedDelivery", shipment.getEstimatedDelivery());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    private void forwardShipmentToEndpoint(Shipment shipment) {
        try {
            log.debug("Forwarding shipment {} to endpoint: {}", shipment.getShipmentId(), shipmentEndpoint);
            restTemplate.postForObject(shipmentEndpoint, shipment, Shipment.class);
            log.info("Successfully forwarded shipment {} to {}", shipment.getShipmentId(), shipmentEndpoint);
        } catch (RestClientException e) {
            log.error("Failed to forward shipment {} to endpoint {}: {}",
                shipment.getShipmentId(), shipmentEndpoint, e.getMessage(), e);
        }
    }
}

