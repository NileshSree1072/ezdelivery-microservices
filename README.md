# EzDelivery – Event-Driven Grocery Platform

Spring Boot microservices backend for the EzDelivery grocery storefront
([ezdelivery](https://github.com/NileshSree1072/ezdelivery)) and admin panel
([ezdeliveryadmin](https://github.com/NileshSree1072/ezdeliveryadmin)).

> 🚧 Work in progress. Stages 1–2 are complete (see the roadmap below).

## How an order works (Saga pattern over Kafka)

Placing an order touches three services that each own their own database, so a single
database transaction is impossible. Instead, each service does its local step and publishes
an event; the next service reacts to it (choreography). If a later step fails, earlier steps
are undone by **compensating actions**.

```
POST /api/orders ──► order-service saves PENDING ──► order.created
                                                          │
                                                   inventory-service
                                       ┌──────────────────┴──────────────────┐
                              stock available                          not enough stock
                       inventory.stock-reserved                   inventory.stock-rejected
                                       │                                      │
                               payment-service                        order CANCELLED
                       ┌───────────────┴───────────────┐
                 charge approved                 charge declined
               payment.succeeded                 payment.failed
                       │                               │
        order CONFIRMED + stock committed    order CANCELLED + stock RELEASED (compensation)
                       │                               │
                       └──────────► order.completed ◄──┘
                                          │
                                 notification-service
```

**Reliability decisions**

| Problem | Solution |
|---|---|
| Kafka can deliver the same event twice | Every consumer is idempotent: `UNIQUE(order_id)` on reservations and payments, and redelivered events reuse the stored result instead of acting again |
| Two orders racing for the last unit | `SELECT ... FOR UPDATE` row locks on inventory, taken in a fixed order to avoid deadlocks |
| Events for one order arriving out of order | Order status only moves forward (`PENDING → STOCK_RESERVED → CONFIRMED/CANCELLED`); late or stale events are ignored, and `@Version` optimistic locking stops concurrent overwrites |
| A message that keeps failing | Retried 3 times, then moved to a `<topic>.DLT` dead-letter topic so it cannot block the partition |
| Ordering per order | Every event is keyed by `orderId`, so all events for one order go to the same partition |
| Client tampering with prices | The order service fetches the current price from the product service; the client only sends product IDs and quantities |

## Services

| Service | Port | Responsibility | Storage |
|---|---|---|---|
| `api-gateway` | 8080 | Single entry point, request routing | – |
| `product-service` | 8081 | Product catalog | MongoDB |
| `inventory-service` | 8082 | Stock levels and reservations | PostgreSQL (`inventory_db`) |
| `order-service` | 8083 | Order lifecycle | PostgreSQL (`order_db`) |
| `payment-service` | 8084 | Stripe payments | PostgreSQL (`payment_db`) |
| `notification-service` | 8085 | Order notifications | – |

Infrastructure: Kafka (KRaft mode), Redis, PostgreSQL, MongoDB. All run with Docker Compose.
Shared event contracts and Kafka settings live in the `common` module.

## Run locally

Requirements: Java 17 and Docker.

```bash
./mvnw package -DskipTests      # Windows: mvnw.cmd package -DskipTests
docker compose up -d --build
```

Check that everything is up:

```bash
curl http://localhost:8080/actuator/health
```

## API (via gateway, port 8080)

| Method | Path | Description |
|---|---|---|
| GET | `/api/products?categoryId=` | List products (optional category filter) |
| GET | `/api/products/{id}` | Get a product |
| POST | `/api/products` | Create a product |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product |

| PUT | `/api/inventory/{productId}` | Set available stock `{"availableQuantity": 10}` |
| GET | `/api/inventory/{productId}` | Available and reserved stock |
| POST | `/api/orders` | Place an order. Returns `202 Accepted` with status `PENDING` |
| GET | `/api/orders/{id}` | Order with its current status |
| GET | `/api/orders?email=` | A customer's orders, newest first |
| GET | `/api/payments/order/{orderId}` | Payment for an order |

### Try the full flow

```bash
# 1. Create a product and give it stock
curl -X POST http://localhost:8080/api/products -H "Content-Type: application/json" \
  -d '{"title":"Organic Bananas","price":60.00}'
curl -X PUT http://localhost:8080/api/inventory/<productId> -H "Content-Type: application/json" \
  -d '{"availableQuantity":10}'

# 2. Place an order (202 Accepted, status PENDING)
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" \
  -d '{"customerEmail":"you@example.com","customerName":"You","items":[{"productId":"<productId>","quantity":3}]}'

# 3. A moment later the saga has finished: CONFIRMED
curl http://localhost:8080/api/orders/<orderId>
```

The local mock payment provider declines charges above Rs. 10,000
(`PAYMENT_MOCK_DECLINE_ABOVE`), which is an easy way to watch the compensation path:
the order is cancelled and its reserved stock is returned.

## Roadmap

- [x] Stage 1 – Multi-module project, API gateway, Docker Compose infrastructure, product catalog API
- [x] Stage 2 – Kafka order saga with compensation, idempotent consumers, dead-letter topics and Flyway migrations
- [ ] Stage 3 – Redis caching and gateway rate limiting
- [ ] Stage 4 – Tests (JUnit, Testcontainers) and GitHub Actions CI
- [ ] Stage 5 – Architecture diagram and API docs
- [ ] Stage 6 – k6 load testing
