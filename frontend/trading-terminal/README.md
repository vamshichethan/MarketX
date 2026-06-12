# MarketX Trading Terminal

Phase 12 adds a Bloomberg-style React trading terminal for MarketX.

The terminal is a Vite + React JavaScript app with Tailwind CSS, Axios, and Recharts. It connects to the MarketX backend services running locally.

## Screens

| Screen | Purpose |
| --- | --- |
| Dashboard | Overview of market prices, PnL, volume, and trade count. |
| Order Entry | Submit BUY/SELL, LIMIT/MARKET orders to OMS. |
| Order Book | View top bids, asks, best bid, best ask, and spread. |
| Positions | View account positions by symbol. |
| PnL | View realized, unrealized, and total PnL. |
| Market Data | View live prices and control the simulated feed. |
| Analytics | View VWAP, spread, volume, and trade count charts. |
| FIX Gateway | Submit simplified FIX messages and view reports. |

## APIs Used

| Service | Base URL | Used For |
| --- | --- | --- |
| OMS | `http://localhost:8080` | Orders, order book, trades. |
| Position Service | `http://localhost:8081` | Positions. |
| PnL Service | `http://localhost:8082` | PnL and market price publishing. |
| Risk Service | `http://localhost:8083` | Available for future terminal risk views. |
| Market Data Service | `http://localhost:8084` | Latest prices and feed controls. |
| Analytics Service | `http://localhost:8085` | Dashboard analytics. |
| FIX Gateway | `http://localhost:8086` | FIX messages and reports. |

## Environment

Copy the example file:

```bash
cp .env.example .env
```

Variables:

```text
VITE_OMS_API=http://localhost:8080
VITE_POSITION_API=http://localhost:8081
VITE_PNL_API=http://localhost:8082
VITE_RISK_API=http://localhost:8083
VITE_MARKET_DATA_API=http://localhost:8084
VITE_ANALYTICS_API=http://localhost:8085
VITE_FIX_API=http://localhost:8086
```

## Run

```bash
npm install
npm run dev
```

The app runs on:

```text
http://localhost:5173
```

## Testing Flow

1. Start MarketX backend services.
2. Start the trading terminal.
3. Open Dashboard and confirm market/analytics cards load.
4. Submit a BUY order from Order Entry.
5. Submit a matching SELL order from another account.
6. Check Order Book, Positions, PnL, and Analytics.
7. Publish or tick market data from Market Data.
8. Submit a sample FIX message from FIX Gateway and review reports.
