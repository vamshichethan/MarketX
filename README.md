# MarketX

MarketX is a learning-first project for building an institutional electronic trading platform from the ground up.

The goal is to understand how real financial markets technology works before writing production-style backend code. MarketX will grow in phases, starting with market foundations and later moving toward order management, risk checks, exchange connectivity, execution processing, positions, PnL, and settlement workflows.

## Why This Project Exists

Electronic trading systems are used by banks, brokers, hedge funds, exchanges, and trading firms to move orders through complex workflows with very high expectations for speed, reliability, and correctness.

MarketX exists to make those systems easier to understand by building them step by step:

- Learn the business concepts first.
- Document how trades move through market systems.
- Design clear system boundaries before coding.
- Build a resume-quality engineering project with realistic financial technology architecture.

## Phase 0: Market Foundations

Phase 0 is documentation only. It explains the core market concepts and the end-to-end trade lifecycle that future MarketX services will eventually model.

This phase covers:

- What stocks, exchanges, brokers, traders, bids, asks, spreads, and order books are.
- How market and limit orders behave.
- How an order moves through an OMS, risk engine, exchange, matching engine, execution reports, position service, PnL service, and settlement process.
- What can go wrong in a trading workflow.
- Why banks care deeply about latency, reliability, and correctness.

## Phase 1: Exchange Simulator Core Engine

Phase 1 adds a command-line mini exchange simulator written in Java. It focuses only on the in-memory core exchange engine: orders, trades, an order book, price-time priority, and a CLI for placing buy and sell orders.

This phase does not use Spring Boot, Kafka, PostgreSQL, Redis, Docker, React, or microservices.

### How to Run the CLI

Compile the Java files:

```bash
javac -d out $(find src/main/java -name "*.java")
```

Start the exchange simulator:

```bash
java -cp out com.marketx.Main
```

### Supported Commands

| Command | Description |
| --- | --- |
| `PLACE BUY LIMIT AAPL 100 150` | Place a buy limit order for 100 shares at 150. |
| `PLACE SELL LIMIT AAPL 100 150` | Place a sell limit order for 100 shares at 150. |
| `PLACE BUY MARKET AAPL 50` | Place a buy market order for 50 shares. |
| `PLACE SELL MARKET AAPL 50` | Place a sell market order for 50 shares. |
| `BOOK AAPL` | Show top 5 bids, top 5 asks, best bid, best ask, and spread. |
| `DEPTH AAPL` | Same as `BOOK AAPL`. |
| `CANCEL ORD-1` | Cancel an active resting limit order. |
| `MODIFY ORD-1 200 151` | Modify an active limit order. The order loses time priority. |
| `ORDER ORD-1` | Show the current state of one order. |
| `REPORTS ORD-1` | Show execution reports for one order. |
| `TRADES` | Show all executed trades. |
| `HELP` | Show available commands. |
| `EXIT` | Stop the CLI. |

### Example Session

```text
MarketX Exchange Simulator
Type HELP to see available commands.

> PLACE BUY LIMIT AAPL 100 150
ORDER ACCEPTED: BUY LIMIT AAPL 100 @ 150

ORDER BOOK: AAPL

BIDS:
Price      Quantity
150.00     100

ASKS:
Price      Quantity
EMPTY

BEST BID: 150.00
BEST ASK: N/A
SPREAD: N/A

> PLACE SELL LIMIT AAPL 100 150
ORDER ACCEPTED: SELL LIMIT AAPL 100 @ 150
TRADE EXECUTED: AAPL 100 @ 150

ORDER BOOK: AAPL

BIDS:
Price      Quantity
EMPTY

ASKS:
Price      Quantity
EMPTY

BEST BID: N/A
BEST ASK: N/A
SPREAD: N/A

> TRADES
TradeId | Symbol | Qty | Price | BuyOrderId | SellOrderId | Time
1       | AAPL   | 100 | 150   | 1          | 2           | 2026-06-12T10:30:00
```

### Price-Time Priority

The order book chooses which orders trade first using price-time priority.

For buy orders:

- Higher prices have priority.
- If two buy orders have the same price, the earlier order has priority.

For sell orders:

- Lower prices have priority.
- If two sell orders have the same price, the earlier order has priority.

### Market Orders vs Limit Orders

| Order Type | Meaning |
| --- | --- |
| Market Order | Trades immediately against available opposite-side orders. Any unfilled quantity is not stored in the book. |
| Limit Order | Trades only at its limit price or better. Any unfilled quantity remains in the book. |

## Phase 2: Order Book Engine

Phase 2 improves the exchange simulator with a clearer real-time market depth view.

Market depth means the visible buy and sell liquidity available at different price levels. Instead of showing every individual order, the CLI now aggregates all remaining quantity at the same price.

Example:

```text
BIDS:
Price      Quantity
150.00     300
149.00     300

ASKS:
Price      Quantity
151.00     100
152.00     200
```

If two buy orders are waiting at `150.00`, one for `100` shares and one for `200` shares, the book shows one price level:

```text
150.00     300
```

### Top 5 Bids and Asks

The `BOOK AAPL` and `DEPTH AAPL` commands show only the top 5 levels on each side:

- Top 5 bids are the highest buy prices currently waiting.
- Top 5 asks are the lowest sell prices currently waiting.

This is how traders often look at the most important part of the order book without reading every order in the market.

### Best Bid, Best Ask, and Spread

| Term | Meaning |
| --- | --- |
| Best Bid | The highest price buyers are currently willing to pay. |
| Best Ask | The lowest price sellers are currently willing to accept. |
| Spread | `Best Ask - Best Bid`. If either side is empty, the spread is `N/A`. |

### Why TreeMap Replaced PriorityQueue for Depth

Phase 1 used `PriorityQueue`, which is good for finding the single best order to match next. However, it is not ideal for printing a full sorted depth view because it does not expose all levels in clean sorted order without copying and sorting.

Phase 2 uses `TreeMap`:

- Buy levels use reverse order, so the highest price is first.
- Sell levels use natural order, so the lowest price is first.
- Each price level stores a FIFO `Queue<Order>`.
- The queue preserves time priority for orders at the same price.

This gives both correct matching and clean market depth display:

```text
BUY side:
150.00 -> [order1, order2]
149.00 -> [order3]

SELL side:
151.00 -> [order4]
152.00 -> [order5]
```

### Phase 2 Sample Session

```text
> PLACE BUY LIMIT AAPL 100 150
ORDER ACCEPTED: BUY LIMIT AAPL 100 @ 150

ORDER BOOK: AAPL

BIDS:
Price      Quantity
150.00     100

ASKS:
Price      Quantity
EMPTY

BEST BID: 150.00
BEST ASK: N/A
SPREAD: N/A

> PLACE BUY LIMIT AAPL 200 150
ORDER ACCEPTED: BUY LIMIT AAPL 200 @ 150

ORDER BOOK: AAPL

BIDS:
Price      Quantity
150.00     300

ASKS:
Price      Quantity
EMPTY

BEST BID: 150.00
BEST ASK: N/A
SPREAD: N/A

> PLACE SELL LIMIT AAPL 100 151
ORDER ACCEPTED: SELL LIMIT AAPL 100 @ 151

ORDER BOOK: AAPL

BIDS:
Price      Quantity
150.00     300

ASKS:
Price      Quantity
151.00     100

BEST BID: 150.00
BEST ASK: 151.00
SPREAD: 1.00
```

## Phase 3: Matching Engine

Phase 3 upgrades MarketX from a simple simulator into a more complete in-memory matching engine.

It adds:

- Order status tracking.
- Correct price-time priority matching.
- Resting order trade price.
- Partial-fill handling.
- Multiple-match handling.
- Cancel support.
- Modify support.
- Order lookup.
- Execution reports.
- Aggressor side on trades.

### Price Priority

The best price always trades first.

For buy orders, the highest bid has priority:

```text
151.00 before 150.00
```

For sell orders, the lowest ask has priority:

```text
149.00 before 150.00
```

### Time Priority

If two orders are at the same price, the older order trades first.

```text
PLACE BUY LIMIT AAPL 100 150
PLACE BUY LIMIT AAPL 100 150
PLACE SELL LIMIT AAPL 150 149
```

The first buy order fills for `100` before the second buy order receives the remaining `50`.

### Resting Order Price Rule

Trades execute at the resting order price.

If the buy order is resting first:

```text
PLACE BUY LIMIT AAPL 100 150
PLACE SELL LIMIT AAPL 100 149
```

The trade price is `150`.

If the sell order is resting first:

```text
PLACE SELL LIMIT AAPL 100 149
PLACE BUY LIMIT AAPL 100 150
```

The trade price is `149`.

### Partial Fills

Orders track both original quantity and remaining quantity.

```text
PLACE BUY LIMIT AAPL 100 150
PLACE SELL LIMIT AAPL 40 149
```

Result:

- Trade executes for `40`.
- Buy order has `60` remaining.
- Buy order status becomes `PARTIALLY_FILLED`.
- Sell order status becomes `FILLED`.

### Cancel Orders

Only active resting limit orders can be cancelled.

```text
CANCEL ORD-1
```

If the order is still active, it is removed from the order book and marked `CANCELLED`.

### Modify Orders

Only active limit orders can be modified.

```text
MODIFY ORD-1 200 151
```

Every modify loses time priority in this phase. The engine removes the old resting order, creates a refreshed version with the same order ID, assigns a new timestamp, and reinserts it. If the modified price crosses the opposite side, it matches immediately.

### Execution Reports

Execution reports describe lifecycle events for an order:

- New accepted order
- Partial fill
- Full fill
- Cancel
- Modify
- Reject

Use:

```text
REPORTS ORD-1
```

### Matching Engine Design Notes

MarketX keeps the engine intentionally small, but the data structures mirror real exchange concerns.

| Design Choice | Why It Matters |
| --- | --- |
| `TreeMap` price levels | Gives efficient access to the best bid or best ask without scanning the whole book. |
| `Queue<Order>` per price | Preserves time priority for orders at the same price. |
| `orderMap` | Allows fast lookup for cancel, modify, order status, and execution reports. |
| Market orders not stored | Market orders are meant to execute immediately; any unfilled quantity is cancelled instead of resting. |
| Aggregated levels | The book can display market depth without printing every individual order. |

The matching loop only looks at the best opposite price level. This avoids unnecessary scanning and keeps the core logic closer to low-latency exchange design.

### Phase 3 Sample Commands

```text
PLACE BUY LIMIT AAPL 100 150
PLACE BUY LIMIT AAPL 100 150
PLACE SELL LIMIT AAPL 150 149
ORDER ORD-1
ORDER ORD-2
TRADES
REPORTS ORD-1
BOOK AAPL
```

Modify losing priority:

```text
PLACE BUY LIMIT AAPL 100 150
PLACE BUY LIMIT AAPL 100 150
MODIFY ORD-1 100 150
PLACE SELL LIMIT AAPL 100 149
```

`ORD-2` fills before modified `ORD-1` because the modify reset `ORD-1`'s time priority.

## Phase 4: OMS Microservice

Phase 4 adds a Spring Boot Order Management System, or OMS, under `backend/oms-service`.

An OMS is the backend service that accepts trader order requests, validates them, stores them, and routes them toward an exchange. Orders should not directly hit the exchange engine because institutions need a controlled entry point for validation, auditing, persistence, client APIs, and future routing to risk checks or messaging systems.

Phase 4 still does not add Kafka, a risk engine, React, or production microservice orchestration. The OMS uses REST, PostgreSQL, Spring Data JPA, and an `ExchangeClient` interface backed by an in-memory exchange adapter.

### Phase 4 Architecture

```mermaid
flowchart LR
    Client["Client / Trader"]
    API["OMS REST API"]
    DB["PostgreSQL"]
    ExchangeClient["ExchangeClient"]
    Engine["In-Memory Matching Engine"]

    Client --> API
    API --> DB
    API --> ExchangeClient
    ExchangeClient --> Engine
```

### OMS API Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/orders` | Create a new order. |
| `PUT` | `/orders/{orderId}` | Modify an active limit order. |
| `DELETE` | `/orders/{orderId}` | Cancel an active order. |
| `GET` | `/orders/{orderId}` | Get one order. |
| `GET` | `/orders` | List orders with optional `symbol`, `side`, and `status` filters. |
| `GET` | `/order-book/{symbol}` | Get top 5 bids, top 5 asks, best bid, best ask, and spread. |
| `GET` | `/trades` | Get executed trades with optional `symbol` filter. |

### Database Schema

The OMS uses PostgreSQL through Spring Data JPA.

`orders`

| Field | Meaning |
| --- | --- |
| `id` | Database primary key. |
| `orderId` | External order ID such as `ORD-1`. |
| `accountId` | Account that owns the order, such as `TRADER-1`. |
| `symbol` | Instrument symbol, such as `AAPL`. |
| `side` | `BUY` or `SELL`. |
| `type` | `MARKET` or `LIMIT`. |
| `originalQuantity` | Quantity requested when the order was created or modified. |
| `remainingQuantity` | Quantity still open. |
| `price` | Limit price, or `0` for market orders. |
| `status` | `NEW`, `PARTIALLY_FILLED`, `FILLED`, `CANCELLED`, or `REJECTED`. |
| `createdAt` | Creation timestamp. |
| `updatedAt` | Last update timestamp. |

`trades`

| Field | Meaning |
| --- | --- |
| `tradeId` | External trade ID such as `TRD-1`. |
| `symbol` | Instrument symbol. |
| `quantity` | Executed quantity. |
| `price` | Execution price. |
| `buyOrderId` | Buy order ID. |
| `sellOrderId` | Sell order ID. |
| `buyAccountId` | Account that owns the buy order. |
| `sellAccountId` | Account that owns the sell order. |
| `aggressorSide` | Incoming order side that caused the match. |
| `executedAt` | Execution timestamp. |

`execution_reports`

| Field | Meaning |
| --- | --- |
| `executionId` | Execution report ID such as `EXE-1`. |
| `orderId` | Related order ID. |
| `symbol` | Instrument symbol. |
| `side` | Order side. |
| `status` | Order status at report time. |
| `executedQuantity` | Quantity executed for the report event. |
| `executedPrice` | Execution price if applicable. |
| `remainingQuantity` | Remaining quantity after the event. |
| `message` | Human-readable event message. |
| `createdAt` | Report timestamp. |

### How to Run PostgreSQL

From the OMS service directory:

```bash
cd backend/oms-service
docker compose up -d postgres
```

The local database config is:

```text
database: marketx_oms
username: marketx
password: marketx
port: 5432
```

### How to Run the OMS

From `backend/oms-service`:

```bash
mvn spring-boot:run
```

The service runs on:

```text
http://localhost:8080
```

### Sample Curl Commands

Create a limit order:

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

Create a market order:

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "TRADER-1",
    "symbol": "AAPL",
    "side": "BUY",
    "type": "MARKET",
    "quantity": 50
  }'
```

Modify an order:

```bash
curl -X PUT http://localhost:8080/orders/ORD-1 \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 200,
    "price": 151.0
  }'
```

Cancel an order:

```bash
curl -X DELETE http://localhost:8080/orders/ORD-1
```

Get one order:

```bash
curl http://localhost:8080/orders/ORD-1
```

List orders:

```bash
curl http://localhost:8080/orders
curl "http://localhost:8080/orders?symbol=AAPL"
curl "http://localhost:8080/orders?status=FILLED"
curl "http://localhost:8080/orders?side=BUY"
```

Get the order book:

```bash
curl http://localhost:8080/order-book/AAPL
```

Get trades:

```bash
curl http://localhost:8080/trades
curl "http://localhost:8080/trades?symbol=AAPL"
```

### Phase 4 Test Flow

1. Start PostgreSQL.
2. Start the OMS service.
3. `POST` a `BUY LIMIT AAPL 100 @ 150`.
4. `POST` a `SELL LIMIT AAPL 100 @ 150`.
5. Confirm a trade appears with `GET /trades`.
6. Check market depth with `GET /order-book/AAPL`.
7. Check persisted orders with `GET /orders`.
8. Modify an active order with `PUT /orders/{orderId}`.
9. Cancel an active order with `DELETE /orders/{orderId}`.

## Phase 5: Position Service

Phase 5 adds a separate Spring Boot Position Service under `backend/position-service`.

The Position Service tracks net holdings by `accountId + symbol` after trades execute.

```text
BUY 100 AAPL  -> +100 AAPL
SELL 40 AAPL -> +60 AAPL
SELL 100 AAPL -> -40 AAPL
```

A positive position is `LONG`, a negative position is `SHORT`, and zero is `FLAT`.

### Phase 5 Architecture

```mermaid
flowchart LR
    OMS["OMS / Exchange"]
    Trade["Trade Executed"]
    Position["Position Service REST API"]
    DB["PostgreSQL"]

    OMS --> Trade
    Trade --> Position
    Position --> DB
```

For now, OMS notifies Position Service by REST after new trades are stored. Later, this can move to Kafka trade events without changing the Position Service position logic.

### Position Service APIs

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/positions/events/trade` | Process an executed trade event. |
| `GET` | `/positions/{accountId}/{symbol}` | Get a position for one account and symbol. |
| `GET` | `/positions/{accountId}` | Get all positions for one account. |
| `GET` | `/positions` | Get all positions. |

### Position Database

The Position Service uses a separate PostgreSQL database:

```text
marketx_positions
```

Main tables:

| Table | Purpose |
| --- | --- |
| `positions` | Stores net quantity, average price, and position type by account and symbol. |
| `processed_trades` | Stores processed trade IDs to prevent duplicate position updates. |

### Position Service Run Commands

Start PostgreSQL:

```bash
cd backend/oms-service
docker compose up -d postgres
```

Run the Position Service:

```bash
cd backend/position-service
mvn spring-boot:run
```

The Position Service runs on:

```text
http://localhost:8081
```

### Position Curl Examples

Process a BUY trade event:

```bash
curl -X POST http://localhost:8081/positions/events/trade \
  -H "Content-Type: application/json" \
  -d '{
    "tradeId": "TRD-1-BUY",
    "accountId": "TRADER-1",
    "symbol": "AAPL",
    "side": "BUY",
    "quantity": 100,
    "price": 150.0,
    "executedAt": "2026-06-09T10:00:00"
  }'
```

Expected position:

```json
{
  "accountId": "TRADER-1",
  "symbol": "AAPL",
  "netQuantity": 100,
  "averagePrice": 150.0,
  "positionType": "LONG"
}
```

Process a SELL trade event:

```bash
curl -X POST http://localhost:8081/positions/events/trade \
  -H "Content-Type: application/json" \
  -d '{
    "tradeId": "TRD-2-SELL",
    "accountId": "TRADER-1",
    "symbol": "AAPL",
    "side": "SELL",
    "quantity": 40,
    "price": 155.0,
    "executedAt": "2026-06-09T10:05:00"
  }'
```

Expected position:

```json
{
  "netQuantity": 60,
  "positionType": "LONG"
}
```

Flip long to short:

```bash
curl -X POST http://localhost:8081/positions/events/trade \
  -H "Content-Type: application/json" \
  -d '{
    "tradeId": "TRD-3-SELL",
    "accountId": "TRADER-1",
    "symbol": "AAPL",
    "side": "SELL",
    "quantity": 100,
    "price": 160.0,
    "executedAt": "2026-06-09T10:10:00"
  }'
```

Expected position:

```json
{
  "netQuantity": -40,
  "positionType": "SHORT"
}
```

Get a position:

```bash
curl http://localhost:8081/positions/TRADER-1/AAPL
```

### OMS Integration

Phase 5 adds `accountId` to OMS order requests and responses.

When OMS stores a newly executed trade, it sends two trade events to Position Service:

- Buyer account receives a `BUY` event.
- Seller account receives a `SELL` event.

The event IDs are suffixed, for example `TRD-1-BUY` and `TRD-1-SELL`, so duplicate protection works per account-side event.

## Documentation

- [How a Trade Happens](docs/phase-0/how-a-trade-happens.md)
- [Glossary](docs/phase-0/glossary.md)
- [Trade Flow Diagram](docs/phase-0/diagrams/trade-flow.md)
- [Order Book Example](docs/phase-0/diagrams/order-book-example.md)

## Future Phases

Future phases may include:

| Phase | Focus |
| --- | --- |
| Phase 1 | Exchange simulator core engine |
| Phase 2 | Order book engine and market depth |
| Phase 3 | Matching engine, order state, cancel, modify, and execution reports |
| Phase 4 | OMS microservice with REST, PostgreSQL, and JPA |
| Phase 5 | Position Service with net long, short, and flat holdings |
| Phase 6 | Settlement, clearing, reliability, and observability |

MarketX currently contains Phase 0 documentation, the Phase 1 in-memory exchange simulator, the Phase 2 market depth order book engine, the Phase 3 matching engine, the Phase 4 OMS microservice, and the Phase 5 Position Service.
