package com.github.drnow4u.kafkasidecar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KafkaMessageConsumer {

    private final List<String> consumedMessages = new ArrayList<>();

    /**
     * Listens to the Kafka topic "events" and processes incoming messages.
     *
     * @param message the message received from Kafka
     */
    @KafkaListener(topics = "events", groupId = "kafka-sidecar-group")
    public void consume(String message) {
        log.info("Consumed message: {}", message);
        consumedMessages.add(message);
    }

    /**
     * Get all consumed messages (useful for testing).
     *
     * @return list of consumed messages
     */
    public List<String> getConsumedMessages() {
        return new ArrayList<>(consumedMessages);
    }

    /**
     * Clear consumed messages (useful for test isolation).
     */
    public void clearConsumedMessages() {
        consumedMessages.clear();
    }
}


