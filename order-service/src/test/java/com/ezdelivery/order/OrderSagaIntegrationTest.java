package com.ezdelivery.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import com.ezdelivery.common.event.Events.PaymentSucceeded;
import com.ezdelivery.common.event.Events.StockRejected;
import com.ezdelivery.common.event.Events.StockReserved;
import com.ezdelivery.common.event.Topics;
import com.ezdelivery.order.client.ProductClient;
import com.ezdelivery.order.client.ProductClient.ProductSummary;
import com.ezdelivery.order.dto.OrderDtos.CreateOrderRequest;
import com.ezdelivery.order.exception.Exceptions.InvalidOrderException;
import com.ezdelivery.order.model.CustomerOrder;
import com.ezdelivery.order.model.OrderStatus;
import com.ezdelivery.order.service.OrderService;

/** Real PostgreSQL and Kafka; the product service is replaced with a mock. */
@SpringBootTest
@Testcontainers
class OrderSagaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @MockBean
    ProductClient productClient;

    @Autowired
    OrderService orders;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaConsumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        when(productClient.findProduct(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            return id.equals("missing") ? Optional.empty()
                    : Optional.of(new ProductSummary(id, "Product " + id, new BigDecimal("60.00")));
        });
        consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer());
        consumer.subscribe(List.of(Topics.ORDER_CREATED, Topics.ORDER_COMPLETED));
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    private CustomerOrder place(int qty) {
        return orders.placeOrder(new CreateOrderRequest("a@b.com", "A", null, null, null, null,
                List.of(new CreateOrderRequest.Item("banana", qty))));
    }

    private OrderStatus statusOf(UUID id) {
        return orders.get(id).getStatus();
    }

    /** Collects messages until one on {@code topic} has the given key. */
    private ConsumerRecord<String, String> awaitMessage(String topic, UUID key) {
        List<ConsumerRecord<String, String>> seen = new ArrayList<>();
        await().atMost(Duration.ofSeconds(20)).until(() -> {
            consumer.poll(Duration.ofMillis(200)).forEach(seen::add);
            return seen.stream().anyMatch(r -> r.topic().equals(topic) && r.key().equals(key.toString()));
        });
        return seen.stream().filter(r -> r.topic().equals(topic) && r.key().equals(key.toString())).findFirst().orElseThrow();
    }

    @Test
    void placingAnOrderSavesItPendingAndPublishesOrderCreated() {
        CustomerOrder order = place(3);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).as("price comes from the product service").isEqualByComparingTo("180.00");
        assertThat(awaitMessage(Topics.ORDER_CREATED, order.getId()).value())
                .contains(order.getId().toString()).contains("\"quantity\":3");
    }

    @Test
    void successfulPaymentConfirmsOrderAndAnnouncesIt() {
        CustomerOrder order = place(1);
        kafkaTemplate.send(Topics.STOCK_RESERVED, order.getId().toString(),
                new StockReserved(order.getId(), "a@b.com", order.getTotalAmount()));
        kafkaTemplate.send(Topics.PAYMENT_SUCCEEDED, order.getId().toString(),
                new PaymentSucceeded(order.getId(), UUID.randomUUID(), order.getTotalAmount()));

        await().atMost(Duration.ofSeconds(20)).until(() -> statusOf(order.getId()) == OrderStatus.CONFIRMED);
        assertThat(awaitMessage(Topics.ORDER_COMPLETED, order.getId()).value()).contains("CONFIRMED");
    }

    @Test
    void outOfStockCancelsOrderWithReason() {
        CustomerOrder order = place(1);
        kafkaTemplate.send(Topics.STOCK_REJECTED, order.getId().toString(),
                new StockRejected(order.getId(), "Insufficient stock for product banana"));

        await().atMost(Duration.ofSeconds(20)).until(() -> statusOf(order.getId()) == OrderStatus.CANCELLED);
        assertThat(orders.get(order.getId()).getFailureReason()).contains("Insufficient stock");
    }

    @Test
    void eventsArrivingOutOfOrderStillEndConfirmed() {
        CustomerOrder order = place(1);
        kafkaTemplate.send(Topics.PAYMENT_SUCCEEDED, order.getId().toString(),
                new PaymentSucceeded(order.getId(), UUID.randomUUID(), order.getTotalAmount()));
        await().atMost(Duration.ofSeconds(20)).until(() -> statusOf(order.getId()) == OrderStatus.CONFIRMED);

        kafkaTemplate.send(Topics.STOCK_RESERVED, order.getId().toString(),
                new StockReserved(order.getId(), "a@b.com", order.getTotalAmount()));

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .until(() -> statusOf(order.getId()) == OrderStatus.CONFIRMED);
    }

    @Test
    void unknownProductIsRejectedBeforeSaving() {
        assertThatThrownBy(() -> orders.placeOrder(new CreateOrderRequest("a@b.com", "A", null, null, null, null,
                List.of(new CreateOrderRequest.Item("missing", 1)))))
                .isInstanceOf(InvalidOrderException.class);
    }
}
