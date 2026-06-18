# MarketX Replay Service

Phase 14 adds a backtesting-style Historical Replay Service for MarketX.

Historical replay loads old market sessions from CSV, replays ticks at the original timing or an accelerated speed, and publishes those ticks back into Kafka as live-looking market data.

## Why Trading Systems Use Replay

Trading teams use replay to:

- Reproduce market sessions during incidents.
- Test analytics and PnL behavior against known price paths.
- Backtest trading logic with deterministic historical inputs.
- Debug risk checks and market data consumers without waiting for live markets.

## CSV Format

```csv
timestamp,symbol,price,volume,bidPrice,askPrice
2026-06-09T09:15:00,AAPL,100.00,1000,99.95,100.05
2026-06-09T09:15:01,AAPL,100.20,1200,100.15,100.25
2026-06-09T09:15:02,AAPL,99.80,900,99.75,99.85
```

The service calculates spread as:

```text
askPrice - bidPrice
```

Bad rows are logged and skipped so one malformed line does not kill the replay service.

## Replay Speed

| Speed | Meaning |
| --- | --- |
| `1x` | Original timing. |
| `2x` | Twice as fast. |
| `5x` | Five times faster. |
| `10x` | Ten times faster. |

Example: a one-second historical gap waits one second at `1x`, 200 ms at `5x`, and 100 ms at `10x`.

## Kafka Topics Produced

| Topic | Payload |
| --- | --- |
| `market-data` | Full replayed market data tick with `source=HISTORICAL_REPLAY` and `replaySessionId`. |
| `market.prices` | Lightweight market price event for existing PnL and Risk consumers. |

## APIs

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/replay/sessions` | Create replay session from CSV file path. |
| `POST` | `/replay/sessions` multipart | Upload CSV file and create replay session. |
| `POST` | `/replay/sessions/{sessionId}/play` | Start or resume replay. |
| `POST` | `/replay/sessions/{sessionId}/pause` | Pause replay and preserve index. |
| `POST` | `/replay/sessions/{sessionId}/stop` | Stop replay and reset index to zero. |
| `POST` | `/replay/sessions/{sessionId}/speed` | Change replay speed. |
| `GET` | `/replay/sessions/{sessionId}` | Get replay session status. |
| `GET` | `/replay/sessions` | List replay sessions. |

## Sample Curl Commands

Create session:

```bash
curl -X POST http://localhost:8087/replay/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "data/replay/sample-aapl-session.csv",
    "speedMultiplier": 1
  }'
```

Play:

```bash
curl -X POST http://localhost:8087/replay/sessions/REPLAY-1/play
```

Pause:

```bash
curl -X POST http://localhost:8087/replay/sessions/REPLAY-1/pause
```

Speed up:

```bash
curl -X POST http://localhost:8087/replay/sessions/REPLAY-1/speed \
  -H "Content-Type: application/json" \
  -d '{
    "speedMultiplier": 5
  }'
```

Stop:

```bash
curl -X POST http://localhost:8087/replay/sessions/REPLAY-1/stop
```

Check status:

```bash
curl http://localhost:8087/replay/sessions/REPLAY-1
```

Upload CSV:

```bash
curl -X POST http://localhost:8087/replay/sessions \
  -F "file=@data/replay/sample-aapl-session.csv" \
  -F "speedMultiplier=5"
```

## Metrics

The Replay Service exposes Prometheus metrics at:

```text
http://localhost:8087/actuator/prometheus
```

Custom metrics:

| Metric | Meaning |
| --- | --- |
| `replay_sessions_started_total` | Replay sessions started or resumed. |
| `replay_ticks_published_total` | Replayed ticks published to Kafka. |
| `replay_active_sessions` | Active replay worker count. |
| `replay_publish_latency_ms_seconds_*` | Replay publish timer series. |

## Testing Flow

1. Start Kafka.
2. Start Analytics Service, PnL Service, Risk Service, Replay Service, and React Trading Terminal.
3. Create a replay session.
4. Press play.
5. Watch Market Data and Analytics update.
6. Change speed to `5x`.
7. Pause, resume, and stop the replay.
