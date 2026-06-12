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
| `BOOK AAPL` | Show the current order book for AAPL. |
| `TRADES` | Show all executed trades. |
| `HELP` | Show available commands. |
| `EXIT` | Stop the CLI. |

### Example Session

```text
MarketX Exchange Simulator
Type HELP to see available commands.

> PLACE BUY LIMIT AAPL 100 150
ORDER ACCEPTED: BUY LIMIT AAPL 100 @ 150

> PLACE SELL LIMIT AAPL 100 150
ORDER ACCEPTED: SELL LIMIT AAPL 100 @ 150
TRADE EXECUTED: AAPL 100 @ 150

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
| Phase 2 | Risk checks and validation |
| Phase 3 | Exchange simulator and matching engine |
| Phase 4 | Execution reports and positions |
| Phase 5 | PnL calculations and market data |
| Phase 6 | Settlement, clearing, reliability, and observability |

MarketX currently contains Phase 0 documentation and the Phase 1 in-memory exchange simulator.
