package com.marketx.position.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class PositionMetricsService {
    private final MeterRegistry registry;
    private final Counter positionUpdates;
    private final Timer positionUpdateLatency;

    public PositionMetricsService(MeterRegistry registry) {
        this.registry = registry;
        // Custom position metrics track trade-driven position mutations and their processing latency.
        this.positionUpdates = Counter.builder("position_updates_total")
                .description("Total position updates processed")
                .register(registry);
        this.positionUpdateLatency = Timer.builder("position_update_latency_ms")
                .description("Latency for position updates")
                .register(registry);
    }

    public Timer.Sample startUpdate() {
        return Timer.start(registry);
    }

    public void recordUpdate(Timer.Sample sample) {
        sample.stop(positionUpdateLatency);
        positionUpdates.increment();
    }
}
