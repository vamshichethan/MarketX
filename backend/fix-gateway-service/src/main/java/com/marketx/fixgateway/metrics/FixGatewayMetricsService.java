package com.marketx.fixgateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class FixGatewayMetricsService {
    private final Counter messagesReceived;
    private final Counter messagesRejected;
    private final Counter executionReportsSent;

    public FixGatewayMetricsService(MeterRegistry registry) {
        // Custom FIX metrics track inbound client flow, rejected messages, and outbound execution reports.
        this.messagesReceived = Counter.builder("fix_messages_received_total")
                .description("Total inbound FIX messages received")
                .register(registry);
        this.messagesRejected = Counter.builder("fix_messages_rejected_total")
                .description("Total inbound FIX messages rejected")
                .register(registry);
        this.executionReportsSent = Counter.builder("fix_execution_reports_sent_total")
                .description("Total outbound FIX execution reports sent")
                .register(registry);
    }

    public void recordMessageReceived() {
        messagesReceived.increment();
    }

    public void recordMessageRejected() {
        messagesRejected.increment();
    }

    public void recordExecutionReportSent() {
        executionReportsSent.increment();
    }
}
