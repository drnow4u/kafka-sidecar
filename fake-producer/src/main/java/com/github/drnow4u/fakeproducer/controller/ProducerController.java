package com.github.drnow4u.fakeproducer.controller;

import com.github.drnow4u.fakeproducer.service.KafkaProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/producer")
public class ProducerController {

    private final KafkaProducerService kafkaProducerService;

    public ProducerController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/order")
    public ResponseEntity<String> produceOrder() {
        kafkaProducerService.produceOrder();
        return ResponseEntity.ok("Order produced successfully");
    }

    @PostMapping("/orders")
    public ResponseEntity<String> produceOrders(@RequestParam(value = "count", defaultValue = "5") int count) {
        if (count <= 0) {
            return ResponseEntity.badRequest().body("Count must be greater than 0");
        }
        kafkaProducerService.produceMultipleOrders(count);
        return ResponseEntity.ok("Produced " + count + " orders successfully");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Producer is running");
    }
}

