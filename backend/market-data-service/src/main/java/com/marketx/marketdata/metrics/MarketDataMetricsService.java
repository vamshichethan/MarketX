package com.marketx.marketdata.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.function.ToDoubleFunction;

@Service
public class MarketDataMetricsService {
    private final MeterRegistry registry;
    private final Counter ticksPublished;
    private final Timer publishLatency;

    public MarketDataMetricsService(MeterRegistry registry) {
        this.registry = registry;
        // Custom market data metrics show feed throughput and per-tick generation/publish latency.
        this.ticksPublished = Counter.builder("market_data_ticks_published_total")
                .description("Total market data ticks generated and published")
                .register(registry);
        this.publishLatency = Timer.builder("market_data_publish_latency_ms")
                .description("Latency for generating and publishing market data ticks")
                .register(registry);
    }

    public Timer.Sample startPublish() {
        return Timer.start(registry);
    }

    public void recordPublishedTick(Timer.Sample sample) {
        recordPublishLatency(sample);
        ticksPublished.increment();
    }

    public void recordPublishLatency(Timer.Sample sample) {
        sample.stop(publishLatency);
    }

    public <T> void registerPublishingEnabledGauge(T stateObject, ToDoubleFunction<T> valueFunction) {
        Gauge.builder("market_data_publishing_enabled", stateObject, valueFunction)
                .description("Whether scheduled market data publishing is enabled")
                .register(registry);
    }
}
