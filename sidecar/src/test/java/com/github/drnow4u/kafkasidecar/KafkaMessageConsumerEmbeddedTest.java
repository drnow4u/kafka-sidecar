package com.github.drnow4u.kafkasidecar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for KafkaMessageConsumer using Spring's @EmbeddedKafka.
 * 
 * This test class verifies that the Kafka consumer correctly receives and processes
 * messages from the "events" topic using Spring's embedded Kafka broker annotation.
 * 
 * This test requires no Docker setup and uses an in-memory Kafka broker for testing.
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {
        "auto.create.topics.enable=true",
        "delete.topic.enable=true"
    },
    topics = {"events"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.group-id=test-consumer-group",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.max-poll-records=100"
})
class KafkaMessageConsumerEmbeddedTest {

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

    /**
     * Test that long messages are handled correctly.
     */
    @Test
    void testConsumeLongMessage() {
        // Given
        String longMessage = "This is a longer message that contains multiple words and " +
            "spans across a longer text to test whether the consumer can handle messages of " +
            "various lengths without any issues.";
        String topic = "events";

        // When
        kafkaTemplate.send(topic, longMessage);

        // Then
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<String> messages = kafkaMessageConsumer.getConsumedMessages();
                assertThat(messages)
                    .hasSize(1)
                    .contains(longMessage);
            });
    }

    /**
     * Test that special characters in messages are preserved.
     */
    @Test
    void testConsumeMessageWithSpecialCharacters() {
        // Given
        String messageWithSpecialChars = "Message with special chars: !@#$%^&*()_+{}|:\"<>?[];',./";
        String topic = "events";

        // When
        kafkaTemplate.send(topic, messageWithSpecialChars);

        // Then
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<String> messages = kafkaMessageConsumer.getConsumedMessages();
                assertThat(messages)
                    .hasSize(1)
                    .contains(messageWithSpecialChars);
            });
    }
}

