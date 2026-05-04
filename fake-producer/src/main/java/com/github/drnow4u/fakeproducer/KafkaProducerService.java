package com.github.drnow4u.fakeproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String ORDER_TOPIC = "orders";
    
    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final Random random = new Random();
    
    private static final String[] PRODUCT_NAMES = {
            "Laptop", "Smartphone", "Headphones", "Monitor", "Keyboard",
            "Mouse", "USB Cable", "Phone Case", "Screen Protector", "Charger"
    };
    
    private static final String[] PRODUCT_IDS = {
            "PROD001", "PROD002", "PROD003", "PROD004", "PROD005",
            "PROD006", "PROD007", "PROD008", "PROD009", "PROD010"
    };
    
    private static final String[] ORDER_STATUSES = {
            "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
    };
    
    private static final String[] CITIES = {
            "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
            "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose"
    };

    public KafkaProducerService(KafkaTemplate<String, Order> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Produces a fake order message to the orders topic
     */
    public CompletableFuture<Void> produceOrder() {
        Order order = generateFakeOrder();
        logger.info("Producing order: {}", order);
        
        Message<Order> message = MessageBuilder
                .withPayload(order)
                .setHeader(KafkaHeaders.TOPIC, ORDER_TOPIC)
                .setHeader(KafkaHeaders.KEY, order.getOrderId())
                .setHeader("X-B3-TraceId", UUID.randomUUID().toString().replace("-", ""))
                .build();
        
        return kafkaTemplate.send(message)
                .thenAccept(result -> {
                    logger.info("Successfully sent order {} to topic {}", 
                        order.getOrderId(), ORDER_TOPIC);
                });
    }

    /**
     * Produces multiple fake orders
     */
    public CompletableFuture<Void> produceMultipleOrders(int count) {
        logger.info("Producing {} orders", count);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            futures.add(produceOrder());
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }


    /**
     * Generates a fake order with random data
     */
    private Order generateFakeOrder() {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String customerId = "CUST-" + (1000 + random.nextInt(9000));
        
        List<CartItem> items = generateFakeCartItems();
        BigDecimal totalAmount = calculateTotal(items);
        String status = ORDER_STATUSES[random.nextInt(ORDER_STATUSES.length)];
        LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(30));
        String shippingAddress = generateFakeAddress();
        
        return new Order(orderId, customerId, items, totalAmount, status, createdAt, shippingAddress);
    }

    /**
     * Generates random cart items
     */
    private List<CartItem> generateFakeCartItems() {
        List<CartItem> items = new ArrayList<>();
        int itemCount = 1 + random.nextInt(5);
        
        for (int i = 0; i < itemCount; i++) {
            int productIndex = random.nextInt(PRODUCT_IDS.length);
            String productId = PRODUCT_IDS[productIndex];
            String productName = PRODUCT_NAMES[productIndex];
            int quantity = 1 + random.nextInt(10);
            BigDecimal price = BigDecimal.valueOf(10 + random.nextDouble() * 990);
            
            items.add(new CartItem(productId, productName, quantity, price));
        }
        
        return items;
    }

    /**
     * Calculates total amount from cart items
     */
    private BigDecimal calculateTotal(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Generates a fake shipping address
     */
    private String generateFakeAddress() {
        int streetNumber = 100 + random.nextInt(9900);
        String city = CITIES[random.nextInt(CITIES.length)];
        int zipCode = 10000 + random.nextInt(90000);
        return streetNumber + " Main St, " + city + ", " + zipCode;
    }
}

