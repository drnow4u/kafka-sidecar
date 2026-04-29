package com.github.drnow4u.kafkasidecar;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class KafkaMessageConsumer {

    private final RestTemplate restTemplate;

    @Value("${kafka.order-endpoint:http://localhost:8080/api/orders}")
    private String orderEndpoint;

    public KafkaMessageConsumer(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "orders", groupId = "kafka-sidecar-group")
    public void consumeOrder(ConsumerRecord<String, Order> consumerRecord) {
        Order order = consumerRecord.value();
        Headers headers = consumerRecord.headers();
        String key = consumerRecord.key();

        Map<String, String> headerMap = extractHeaders(headers);
        if (key != null) {
            headerMap.put("X-Kafka-Key", Objects.toString(key, ""));
        }
        log.info("Consumed order: {} with key: {} and headers: {}", order, key, headerMap);

        forwardOrderToEndpoint(order, headerMap);
    }

    private Map<String, String> extractHeaders(Headers headers) {
        Map<String, String> headerMap = new HashMap<>();
        for (Header header : headers) {
            String value = header.value() != null
                ? new String(header.value(), StandardCharsets.UTF_8)
                : null;
            headerMap.put(header.key(), value);
        }
        return headerMap;
    }

    private void forwardOrderToEndpoint(Order order, Map<String, String> headers) {
        try {
            log.debug("Forwarding order {} to endpoint: {} with headers: {}",
                order.getOrderId(), orderEndpoint, headers);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);


            headers.forEach((headerKey, value) -> {
                if (value != null) {
                    httpHeaders.add(headerKey, value);
                }
            });

            HttpEntity<Order> requestEntity = new HttpEntity<>(order, httpHeaders);
            restTemplate.postForObject(orderEndpoint, requestEntity, Order.class);
            log.info("Successfully forwarded order {} to {}", order.getOrderId(), orderEndpoint);
        } catch (RestClientException e) {
            log.error("Failed to forward order {} to endpoint {}: {}", 
                order.getOrderId(), orderEndpoint, e.getMessage(), e);
        }
    }
}

