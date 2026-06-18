package com.marketx.oms.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class OmsMetricsService {
    private final MeterRegistry registry;
    private final Counter ordersSubmitted;
    private final Counter ordersRejected;
    private final Counter ordersFilled;
    private final Timer orderProcessingLatency;

    public OmsMetricsService(MeterRegistry registry) {
        this.registry = registry;
        // Custom trading metrics exported at /actuator/prometheus for throughput and OMS latency dashboards.
        this.ordersSubmitted = Counter.builder("orders_submitted_total")
                .description("Total orders accepted by OMS for processing")
                .register(registry);
        this.ordersRejected = Counter.builder("orders_rejected_total")
                .description("Total orders rejected by validation or risk")
                .register(registry);
        this.ordersFilled = Counter.builder("orders_filled_total")
                .description("Total order sides filled from executed trades")
                .register(registry);
        this.orderProcessingLatency = Timer.builder("order_processing_latency_ms")
                .description("Latency for OMS order processing")
                .register(registry);
    }

    public Timer.Sample startOrderProcessing() {
        return Timer.start(registry);
    }

    public void recordOrderProcessing(Timer.Sample sample) {
        sample.stop(orderProcessingLatency);
    }

    public void recordOrderSubmitted() {
        ordersSubmitted.increment();
    }

    public void recordOrderRejected() {
        ordersRejected.increment();
    }

    public void recordOrdersFilled(double count) {
        ordersFilled.increment(count);
    }
}
