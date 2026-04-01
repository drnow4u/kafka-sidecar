package com.github.drnow4u.kafkasidecar;

import com.github.drnow4u.kafkasidecar.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KafkaMessageConsumer {

    private final List<Order> consumedOrders = new ArrayList<>();

    @KafkaListener(topics = "orders", groupId = "kafka-sidecar-group")
    public void consumeOrder(Order order) {
        log.info("Consumed order: {}", order);
        consumedOrders.add(order);
    }

    public List<Order> getConsumedOrders() {
        return new ArrayList<>(consumedOrders);
    }

    public void clearConsumedOrders() {
        consumedOrders.clear();
    }
}


