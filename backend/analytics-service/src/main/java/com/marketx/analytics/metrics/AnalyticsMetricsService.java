package com.marketx.analytics.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsMetricsService {
    private final MeterRegistry registry;
    private final Counter eventsConsumed;
    private final Timer updateLatency;

    public AnalyticsMetricsService(MeterRegistry registry) {
        this.registry = registry;
        // Custom analytics metrics show consumed event volume and symbol metric update latency.
        this.eventsConsumed = Counter.builder("analytics_events_consumed_total")
                .description("Total market data and trade events consumed by analytics")
                .register(registry);
        this.updateLatency = Timer.builder("analytics_update_latency_ms")
                .description("Latency for analytics updates")
                .register(registry);
    }

    public Timer.Sample startUpdate() {
        return Timer.start(registry);
    }

    public void recordUpdate(Timer.Sample sample) {
        sample.stop(updateLatency);
        eventsConsumed.increment();
    }
}
