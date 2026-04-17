package com.github.drnow4u.kafkasidecar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KafkaMessageConsumer {

    private final List<Order> consumedOrders = new ArrayList<>();
    private final RestTemplate restTemplate;

    @Value("${kafka.order-endpoint:http://localhost:8080/api/orders}")
    private String orderEndpoint;

    public KafkaMessageConsumer(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "orders", groupId = "kafka-sidecar-group")
    public void consumeOrder(Order order) {
        log.info("Consumed order: {}", order);
        consumedOrders.add(order);
        forwardOrderToEndpoint(order);
    }

    private void forwardOrderToEndpoint(Order order) {
        try {
            log.debug("Forwarding order {} to endpoint: {}", order.getOrderId(), orderEndpoint);
            restTemplate.postForObject(orderEndpoint, order, Order.class);
            log.info("Successfully forwarded order {} to {}", order.getOrderId(), orderEndpoint);
        } catch (RestClientException e) {
            log.error("Failed to forward order {} to endpoint {}: {}", 
                order.getOrderId(), orderEndpoint, e.getMessage(), e);
        }
    }

    public List<Order> getConsumedOrders() {
        return new ArrayList<>(consumedOrders);
    }

    public void clearConsumedOrders() {
        consumedOrders.clear();
    }
}


