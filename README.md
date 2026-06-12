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
| Phase 4 | Execution reports and positions |
| Phase 5 | PnL calculations and market data |
| Phase 6 | Settlement, clearing, reliability, and observability |

MarketX currently contains Phase 0 documentation, the Phase 1 in-memory exchange simulator, the Phase 2 market depth order book engine, and the Phase 3 matching engine.
