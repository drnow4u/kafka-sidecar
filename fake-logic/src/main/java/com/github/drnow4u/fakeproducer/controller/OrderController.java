package com.github.drnow4u.fakeproducer.controller;

import com.github.drnow4u.fakeproducer.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleOrder(@RequestBody Order order) {
        log.info("Received order: {}", order);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Order received and processed");
        response.put("orderId", order.getOrderId());
        response.put("customerId", order.getCustomerId());
        response.put("totalAmount", order.getTotalAmount());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}

