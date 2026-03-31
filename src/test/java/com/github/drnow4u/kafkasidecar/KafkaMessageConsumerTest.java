package com.github.drnow4u.kafkasidecar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for KafkaMessageConsumer using Testcontainers.
 * 
 * This test class verifies that the Kafka consumer correctly receives and processes
 * messages from the "events" topic using Testcontainers for a real Kafka instance.
 */
@SpringBootTest
@Testcontainers
class KafkaMessageConsumerTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaMessageConsumer kafkaMessageConsumer;

    @BeforeEach
    void setUp() {
        // Clear consumed messages before each test
        kafkaMessageConsumer.clearConsumedMessages();
    }

    /**
     * Test that a single message is consumed correctly from the Kafka topic.
     */
    @Test
    void testConsumeMessage() {
        // Given
        String testMessage = "Test Kafka Message";
        String topic = "events";

        // When
        kafkaTemplate.send(topic, testMessage);

        // Then - wait for message to be consumed and verify
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<String> messages = kafkaMessageConsumer.getConsumedMessages();
                assertThat(messages)
                    .hasSize(1)
                    .contains(testMessage);
            });
    }

    /**
     * Test that multiple messages are consumed correctly from the Kafka topic.
     */
    @Test
    void testConsumeMultipleMessages() {
        // Given
        String topic = "events";
        String[] messages = {
            "Message 1",
            "Message 2",
            "Message 3"
        };

        // When
        for (String message : messages) {
            kafkaTemplate.send(topic, message);
        }

        // Then - wait for all messages to be consumed
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<String> consumedMessages = kafkaMessageConsumer.getConsumedMessages();
                assertThat(consumedMessages)
                    .hasSize(3)
                    .containsExactlyInAnyOrder(messages);
            });
    }

    /**
     * Test that the consumer is properly configured and autowired.
     */
    @Test
    void testConsumerBeanExists() {
        // Verify consumer bean is autowired
        assertThat(kafkaMessageConsumer).isNotNull();
    }


    /**
     * Test that KafkaTemplate is properly configured.
     */
    @Test
    void testKafkaTemplateExists() {
        // Verify KafkaTemplate is autowired
        assertThat(kafkaTemplate).isNotNull();
    }

    /**
     * Test message ordering - messages should be consumed in the order they were sent.
     */
    @Test
    void testMessageOrdering() {
        // Given
        String topic = "events";
        String[] messages = {
            "First Message",
            "Second Message",
            "Third Message",
            "Fourth Message"
        };

        // When
        for (String message : messages) {
            kafkaTemplate.send(topic, message);
        }

        // Then - verify messages are consumed in order
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<String> consumedMessages = kafkaMessageConsumer.getConsumedMessages();
                assertThat(consumedMessages)
                    .hasSize(4)
                    .containsExactly(messages);
            });
    }

    /**
     * Test that empty string messages are handled correctly.
     */
    @Test
    void testConsumeEmptyMessage() {
        // Given
        String topic = "events";
        String emptyMessage = "";

        // When
        kafkaTemplate.send(topic, emptyMessage);

        // Then
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<String> messages = kafkaMessageConsumer.getConsumedMessages();
                assertThat(messages)
                    .hasSize(1)
                    .contains(emptyMessage);
            });
    }

    /**
     * Test consumer group configuration.
     */
    @Test
    void testConsumerGroupConfiguration() {
        // Verify consumer bean is properly configured
        assertThat(kafkaMessageConsumer).isNotNull();
        assertThat(kafkaMessageConsumer.getConsumedMessages()).isNotNull().isEmpty();
    }
}