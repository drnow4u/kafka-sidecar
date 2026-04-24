package com.github.drnow4u.fakeproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderProducerScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OrderProducerScheduler.class);

    private final KafkaProducerService kafkaProducerService;

    public OrderProducerScheduler(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 1000)
    public void produceOrder() {
        try {
            logger.debug("Scheduled order production triggered");
            kafkaProducerService.produceOrder();
        } catch (Exception e) {
            logger.error("Error producing scheduled order", e);
        }
    }
}

