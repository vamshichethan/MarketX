package com.marketx.pnl.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class PnlMetricsService {
    private final MeterRegistry registry;
    private final Counter pnlUpdates;
    private final Timer calculationLatency;

    public PnlMetricsService(MeterRegistry registry) {
        this.registry = registry;
        // Custom PnL metrics track trade/price-driven recalculations and calculation latency.
        this.pnlUpdates = Counter.builder("pnl_updates_total")
                .description("Total PnL updates processed")
                .register(registry);
        this.calculationLatency = Timer.builder("pnl_calculation_latency_ms")
                .description("Latency for PnL calculations")
                .register(registry);
    }

    public Timer.Sample startCalculation() {
        return Timer.start(registry);
    }

    public void recordUpdate(Timer.Sample sample) {
        recordCalculationLatency(sample);
        pnlUpdates.increment();
    }

    public void recordUpdates(Timer.Sample sample, int count) {
        recordCalculationLatency(sample);
        pnlUpdates.increment(count);
    }

    public void recordCalculationLatency(Timer.Sample sample) {
        sample.stop(calculationLatency);
    }
}
