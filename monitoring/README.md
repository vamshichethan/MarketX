# MarketX Monitoring

Phase 13 adds production-style monitoring for the MarketX trading system with Prometheus, Grafana, Spring Boot Actuator, and Micrometer.

## What Prometheus Does

Prometheus scrapes metrics from every Spring Boot service through `/actuator/prometheus`. It stores time-series data for throughput, latency, errors, CPU, memory, and custom trading metrics.

## What Grafana Does

Grafana reads Prometheus data and turns it into dashboards. The MarketX dashboard is provisioned automatically when Grafana starts.

## Why Trading Systems Need Monitoring

Trading platforms must reveal operational problems quickly:

- Order flow drops can mean client connectivity or OMS issues.
- Latency spikes can hurt execution quality.
- Rejection spikes can indicate bad limits, stale market data, or downstream outages.
- CPU and memory pressure can forecast instability.
- FIX message rates help monitor external connectivity and client flow.

## Start Monitoring

Start all backend services first, then run:

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

Prometheus:

```text
http://localhost:9090
```

Grafana:

```text
http://localhost:3000
```

Default Grafana login:

```text
admin / admin
```

## Important Metrics

| Metric | Service | Meaning |
| --- | --- | --- |
| `orders_submitted_total` | OMS | Orders accepted by OMS for processing. |
| `orders_rejected_total` | OMS | Orders rejected by validation or risk. |
| `orders_filled_total` | OMS | Filled order sides from executed trades. |
| `order_processing_latency_ms_seconds_*` | OMS | OMS order processing timer series. |
| `risk_checks_total` | Risk | Pre-trade risk checks evaluated. |
| `risk_approved_total` | Risk | Risk checks approved. |
| `risk_rejected_total` | Risk | Risk checks rejected. |
| `risk_check_latency_ms_seconds_*` | Risk | Risk evaluation timer series. |
| `market_data_ticks_published_total` | Market Data | Market data ticks published. |
| `market_data_publish_latency_ms_seconds_*` | Market Data | Tick generation/publish timer series. |
| `market_data_publishing_enabled` | Market Data | Gauge showing whether scheduled publishing is enabled. |
| `fix_messages_received_total` | FIX Gateway | Inbound FIX messages received. |
| `fix_messages_rejected_total` | FIX Gateway | Inbound FIX messages rejected. |
| `fix_execution_reports_sent_total` | FIX Gateway | Outbound FIX execution reports sent. |
| `analytics_events_consumed_total` | Analytics | Market/trade events consumed by analytics. |
| `analytics_update_latency_ms_seconds_*` | Analytics | Analytics update timer series. |
| `position_updates_total` | Position | Completed position updates. |
| `position_update_latency_ms_seconds_*` | Position | Position update timer series. |
| `pnl_updates_total` | PnL | Completed PnL updates. |
| `pnl_calculation_latency_ms_seconds_*` | PnL | PnL calculation timer series. |
| `replay_sessions_started_total` | Replay | Replay sessions started or resumed. |
| `replay_ticks_published_total` | Replay | Historical ticks published to Kafka. |
| `replay_active_sessions` | Replay | Active replay worker count. |
| `replay_publish_latency_ms_seconds_*` | Replay | Replay tick publish timer series. |

Spring Boot Actuator also exposes JVM, process, HTTP, health, and datasource metrics.

## Dashboard Panels

The provisioned Grafana dashboard is named:

```text
MarketX Trading System Monitoring
```

Panels:

| Panel | Query |
| --- | --- |
| Order Throughput | `rate(orders_submitted_total[1m])` |
| Risk Rejection Rate | `rate(risk_rejected_total[1m])` |
| Order Processing Latency | `order_processing_latency_ms_seconds_sum / order_processing_latency_ms_seconds_count` |
| Error Rate | `rate(http_server_requests_seconds_count{status=~"5.."}[1m])` |
| CPU Usage | `process_cpu_usage` |
| Memory Usage | `jvm_memory_used_bytes` |
| Market Data Tick Rate | `rate(market_data_ticks_published_total[1m])` |
| FIX Messages Received | `rate(fix_messages_received_total[1m])` |

## Testing Flow

1. Start PostgreSQL and Kafka.
2. Start all MarketX backend services.
3. Start monitoring:

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

4. Open Prometheus and search:

```text
orders_submitted_total
risk_checks_total
market_data_ticks_published_total
```

5. Open Grafana and view the MarketX dashboard.
6. Submit a few orders through the trading terminal or curl.
7. Confirm the throughput, rejection, latency, market data, and FIX panels move.

## Useful Health URLs

```text
http://localhost:8080/actuator/health
http://localhost:8081/actuator/health
http://localhost:8082/actuator/health
http://localhost:8083/actuator/health
http://localhost:8084/actuator/health
http://localhost:8085/actuator/health
http://localhost:8086/actuator/health
http://localhost:8087/actuator/health
```
