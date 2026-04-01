package com.github.drnow4u.kafkasidecar;

import com.github.drnow4u.kafkasidecar.model.CartItem;
import com.github.drnow4u.kafkasidecar.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {
        "auto.create.topics.enable=true",
        "delete.topic.enable=true"
    },
    topics = {"orders"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.group-id=kafka-sidecar-group",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.max-poll-records=100"
})
class KafkaMessageConsumerEmbeddedTest {

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    @Autowired
    private KafkaMessageConsumer kafkaMessageConsumer;

    @BeforeEach
    void setUp() {
        kafkaMessageConsumer.clearConsumedOrders();
    }

    @Test
    void testConsumeOrder() {
        // Given
        Order testOrder = createTestOrder("ORD-001");
        String topic = "orders";

        // When
        kafkaTemplate.send(topic, testOrder.getOrderId(), testOrder);

        // Then
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<Order> orders = kafkaMessageConsumer.getConsumedOrders();
                assertThat(orders)
                    .hasSize(1)
                    .contains(testOrder);
            });
    }

    @Test
    void testConsumeMultipleOrders() {
        // Given
        String topic = "orders";
        Order order1 = createTestOrder("ORD-001");
        Order order2 = createTestOrder("ORD-002");
        Order order3 = createTestOrder("ORD-003");

        // When
        kafkaTemplate.send(topic, order1.getOrderId(), order1);
        kafkaTemplate.send(topic, order2.getOrderId(), order2);
        kafkaTemplate.send(topic, order3.getOrderId(), order3);

        // Then
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<Order> orders = kafkaMessageConsumer.getConsumedOrders();
                assertThat(orders).hasSize(3);
            });
    }

    @Test
    void testConsumerBeanExists() {
        assertThat(kafkaMessageConsumer).isNotNull();
    }

    @Test
    void testKafkaTemplateExists() {
        assertThat(kafkaTemplate).isNotNull();
    }

    private Order createTestOrder(String orderId) {
        List<CartItem> items = List.of(
            new CartItem("PROD001", "Laptop", 1, BigDecimal.valueOf(999.99))
        );
        return new Order(
            orderId,
            "CUST-001",
            items,
            BigDecimal.valueOf(999.99),
            "PENDING",
            LocalDateTime.now(),
            "123 Main St, Test City, 12345"
        );
    }
}

