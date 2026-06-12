# Order Book Example

An order book shows buy orders and sell orders waiting in the market.

Buy orders are called bids. Sell orders are called asks.

## Example Order Book

Symbol: `AAPL`

### Bids

| Price | Quantity |
| ---: | ---: |
| 189.95 | 200 |
| 189.90 | 500 |
| 189.85 | 300 |

### Asks

| Price | Quantity |
| ---: | ---: |
| 190.05 | 100 |
| 190.10 | 300 |
| 190.15 | 400 |

## Best Bid

The best bid is the highest price that any buyer is currently willing to pay.

In this example:

```text
Best Bid = 189.95
```

## Best Ask

The best ask is the lowest price that any seller is currently willing to accept.

In this example:

```text
Best Ask = 190.05
```

## Spread

The spread is the difference between the best ask and the best bid.

```text
Spread = Best Ask - Best Bid
Spread = 190.05 - 189.95
Spread = 0.10
```

## When a Trade Executes

A trade executes when a buy order and sell order are compatible.

For example, a new buy order can trade immediately if its price is greater than or equal to the best ask.

```text
BUY 100 AAPL LIMIT 190.05
```

This can trade because the best ask is 190.05.

A new buy order at 190.00 would not trade immediately:

```text
BUY 100 AAPL LIMIT 190.00
```

That order would wait in the order book because sellers are currently asking at least 190.05.

## Why the Order Book Matters

The order book helps trading systems understand:

- The best available price to buy or sell.
- How much quantity is available at each price.
- Whether an order will execute immediately or wait.
- How liquid the market is.
