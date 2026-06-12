# MarketX OMS Service

The OMS accepts trader orders, stores them, and coordinates routing to the embedded exchange engine.

In Phase 8, new orders are asynchronous:

```text
POST /orders
  -> save order as PENDING_RISK
  -> publish OrderSubmittedEvent
  -> return immediately
```

## Kafka

| Direction | Topic | Event |
| --- | --- | --- |
| Produces | `orders.submitted` | `OrderSubmittedEvent` |
| Consumes | `orders.risk.approved` | `OrderRiskApprovedEvent` |
| Consumes | `orders.risk.rejected` | `OrderRiskRejectedEvent` |
| Produces | `trades.executed` | `TradeExecutedEvent` |
| Consumes | `trades.executed` | `TradeExecutedEvent` |

Consumer group:

```text
oms-service-group
```

OMS stores processed Kafka event IDs in `processed_events` to avoid duplicate processing.

## Run

```bash
cd backend/common-events
mvn install
```

```bash
cd backend/oms-service
mvn spring-boot:run
```

OMS runs on `http://localhost:8080`.

## Submit Order

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "TRADER-1",
    "symbol": "AAPL",
    "side": "BUY",
    "type": "LIMIT",
    "quantity": 100,
    "price": 150.0
  }'
```

Immediate response status is `PENDING_RISK`. Use `GET /orders/{orderId}` later to see the final state.
