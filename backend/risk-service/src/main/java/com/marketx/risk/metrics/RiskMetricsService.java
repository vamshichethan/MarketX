package com.marketx.risk.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class RiskMetricsService {
    private final MeterRegistry registry;
    private final Counter riskChecks;
    private final Counter riskApproved;
    private final Counter riskRejected;
    private final Timer riskCheckLatency;

    public RiskMetricsService(MeterRegistry registry) {
        this.registry = registry;
        // Custom risk metrics track pre-trade decision throughput, rejection pressure, and check latency.
        this.riskChecks = Counter.builder("risk_checks_total")
                .description("Total pre-trade risk checks evaluated")
                .register(registry);
        this.riskApproved = Counter.builder("risk_approved_total")
                .description("Total risk checks approved")
                .register(registry);
        this.riskRejected = Counter.builder("risk_rejected_total")
                .description("Total risk checks rejected")
                .register(registry);
        this.riskCheckLatency = Timer.builder("risk_check_latency_ms")
                .description("Latency for pre-trade risk checks")
                .register(registry);
    }

    public Timer.Sample startRiskCheck() {
        return Timer.start(registry);
    }

    public void recordRiskCheck(Timer.Sample sample, boolean approved) {
        sample.stop(riskCheckLatency);
        riskChecks.increment();
        if (approved) {
            riskApproved.increment();
        } else {
            riskRejected.increment();
        }
    }
}
