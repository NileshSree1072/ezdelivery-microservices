# EzDelivery – Event-Driven Grocery Platform

Spring Boot microservices backend for the EzDelivery grocery storefront
([ezdelivery](https://github.com/NileshSree1072/ezdelivery)) and admin panel
([ezdeliveryadmin](https://github.com/NileshSree1072/ezdeliveryadmin)).

> 🚧 Work in progress. Stage 1 (project setup) is complete.

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

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"title":"Organic Bananas","price":60.00,"properties":{"unit":"dozen"}}'
```

## Roadmap

- [x] Stage 1 – Multi-module project, API gateway, Docker Compose infrastructure, product catalog API
- [ ] Stage 2 – Kafka order saga: order → stock reservation → payment → confirmation / compensation
- [ ] Stage 3 – Redis caching and gateway rate limiting
- [ ] Stage 4 – Tests (JUnit, Testcontainers) and GitHub Actions CI
- [ ] Stage 5 – Architecture diagram and API docs
- [ ] Stage 6 – k6 load testing
