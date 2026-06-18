package com.marketx.replay.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ReplayMetricsService {
    private final MeterRegistry registry;
    private final Counter sessionsStarted;
    private final Counter ticksPublished;
    private final Timer publishLatency;
    private final AtomicInteger activeSessions = new AtomicInteger(0);

    public ReplayMetricsService(MeterRegistry registry) {
        this.registry = registry;
        // Replay metrics show backtest session starts, active workers, tick throughput, and publish latency.
        this.sessionsStarted = Counter.builder("replay_sessions_started_total")
                .description("Total replay sessions started or resumed")
                .register(registry);
        this.ticksPublished = Counter.builder("replay_ticks_published_total")
                .description("Total historical ticks published to Kafka")
                .register(registry);
        this.publishLatency = Timer.builder("replay_publish_latency_ms")
                .description("Latency for publishing replay ticks to Kafka")
                .register(registry);
        Gauge.builder("replay_active_sessions", activeSessions, AtomicInteger::get)
                .description("Active replay worker sessions")
                .register(registry);
    }

    public Timer.Sample startPublish() {
        return Timer.start(registry);
    }

    public void recordTickPublished(Timer.Sample sample) {
        sample.stop(publishLatency);
        ticksPublished.increment();
    }

    public void recordSessionStarted() {
        sessionsStarted.increment();
        activeSessions.incrementAndGet();
    }

    public void recordSessionFinished() {
        activeSessions.updateAndGet(value -> Math.max(0, value - 1));
    }
}
