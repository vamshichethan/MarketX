package com.marketx.fixgateway.service;

import com.marketx.common.events.KafkaTopics;
import com.marketx.fixgateway.dto.FixExecutionReportResponse;
import com.marketx.fixgateway.dto.FixMessageResponse;
import com.marketx.fixgateway.dto.OrderCancelledEvent;
import com.marketx.fixgateway.dto.OrderRiskRejectedEvent;
import com.marketx.fixgateway.dto.TradeExecutedEvent;
import com.marketx.fixgateway.entity.FixMessageEntity;
import com.marketx.fixgateway.enums.FixDirection;
import com.marketx.fixgateway.enums.FixMessageStatus;
import com.marketx.fixgateway.metrics.FixGatewayMetricsService;
import com.marketx.fixgateway.parser.FixMessageBuilder;
import com.marketx.fixgateway.parser.FixMessageParser;
import com.marketx.fixgateway.parser.FixMessageValidator;
import com.marketx.fixgateway.producer.FixGatewayProducer;
import com.marketx.fixgateway.repository.FixMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FixGatewayService {
    private static final Logger log = LoggerFactory.getLogger(FixGatewayService.class);
    private static final DateTimeFormatter FIX_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss");

    private final AtomicLong executionSequence = new AtomicLong(1);
    private final FixMessageParser parser;
    private final FixMessageValidator validator;
    private final FixMessageBuilder builder;
    private final FixGatewayProducer producer;
    private final FixMessageRepository repository;
    private final FixGatewayMetricsService metricsService;

    public FixGatewayService(
            FixMessageParser parser,
            FixMessageValidator validator,
            FixMessageBuilder builder,
            FixGatewayProducer producer,
            FixMessageRepository repository,
            FixGatewayMetricsService metricsService
    ) {
        this.parser = parser;
        this.validator = validator;
        this.builder = builder;
        this.producer = producer;
        this.repository = repository;
        this.metricsService = metricsService;
    }

    @Transactional
    public FixMessageResponse processInbound(String rawMessage) {
        metricsService.recordMessageReceived();
        Map<String, String> tags;
        try {
            tags = parser.parse(rawMessage);
            validator.validate(tags);
        } catch (IllegalArgumentException exception) {
            saveInbound(rawMessage, "UNKNOWN", null, FixMessageStatus.REJECTED, exception.getMessage());
            metricsService.recordMessageRejected();
            return new FixMessageResponse(false, null, "FIX message rejected", null, null, exception.getMessage());
        }

        String messageType = tags.get("35");
        String clOrdId = tags.get("11");
        log.info("FIX message received messageType={} clOrdId={}", messageType, clOrdId);
        try {
            producer.publishInboundFix(clOrdId, rawMessage);
            if ("D".equals(messageType)) {
                publishNewOrder(tags);
                saveInbound(rawMessage, messageType, clOrdId, FixMessageStatus.ACCEPTED, null);
                return new FixMessageResponse(true, messageType, "New Order Single accepted", KafkaTopics.ORDERS_SUBMITTED, clOrdId, null);
            }
            if ("F".equals(messageType)) {
                publishCancel(tags);
                saveInbound(rawMessage, messageType, clOrdId, FixMessageStatus.ACCEPTED, null);
                return new FixMessageResponse(true, messageType, "Order Cancel Request accepted", KafkaTopics.ORDERS_CANCEL_REQUESTED, clOrdId, null);
            }
            throw new IllegalArgumentException("Unsupported FIX message type 35=" + messageType);
        } catch (RuntimeException exception) {
            saveInbound(rawMessage, messageType, clOrdId, FixMessageStatus.REJECTED, exception.getMessage());
            metricsService.recordMessageRejected();
            return new FixMessageResponse(false, messageType, "FIX message rejected", null, clOrdId, exception.getMessage());
        }
    }

    @Transactional
    public void processTradeExecuted(TradeExecutedEvent event) {
        if (event.quantity() == null || event.price() == null) {
            log.warn("Skipping malformed trade event eventId={} tradeId={}", event.eventId(), event.tradeId());
            return;
        }

        createFillReport(event.buyAccountId(), event.buyOrderId(), event.tradeId() + "-BUY", event.symbol(), "1", event.quantity(), event.price(), event.executedAt());
        createFillReport(event.sellAccountId(), event.sellOrderId(), event.tradeId() + "-SELL", event.symbol(), "2", event.quantity(), event.price(), event.executedAt());
    }

    @Transactional
    public void processRiskRejected(OrderRiskRejectedEvent event) {
        String reason = event.reasons() == null || event.reasons().isEmpty()
                ? "Order rejected"
                : String.join("; ", event.reasons());
        String side = sideForOrder(event.orderId()).getOrDefault("54", "1");
        createRejectedReport(event.accountId(), event.orderId(), nextExecId(), event.symbol(), side, reason, event.rejectedAt());
    }

    @Transactional
    public void processOrderCancelled(OrderCancelledEvent event) {
        String side = toFixSide(event.side());
        createCancelledReport(event.accountId(), event.orderId(), nextExecId(), event.symbol(), side, event.message(), event.cancelledAt());
    }

    public List<FixExecutionReportResponse> getReports() {
        return repository.findByDirectionOrderByCreatedAtDesc(FixDirection.OUTBOUND).stream()
                .map(this::toReportResponse)
                .toList();
    }

    public List<FixExecutionReportResponse> getReportsForOrder(String clOrdId) {
        return repository.findByDirectionAndClOrdIdOrderByCreatedAtDesc(FixDirection.OUTBOUND, clOrdId).stream()
                .map(this::toReportResponse)
                .toList();
    }

    private void publishNewOrder(Map<String, String> tags) {
        String ordType = toOrderType(tags.get("40"));
        BigDecimal price = "LIMIT".equals(ordType) ? new BigDecimal(tags.get("44")) : null;
        producer.publishOrderSubmitted(new com.marketx.common.events.OrderSubmittedEvent(
                UUID.randomUUID().toString(),
                tags.get("11"),
                tags.get("49"),
                tags.get("55").toUpperCase(),
                toOrderSide(tags.get("54")),
                ordType,
                Integer.parseInt(tags.get("38")),
                price,
                parseFixTime(tags.get("60"))
        ));
    }

    private void publishCancel(Map<String, String> tags) {
        producer.publishCancelRequested(new com.marketx.common.events.OrderCancelRequestedEvent(
                UUID.randomUUID().toString(),
                tags.get("11"),
                tags.get("41"),
                tags.get("49"),
                tags.get("55").toUpperCase(),
                toOrderSide(tags.get("54")),
                parseFixTime(tags.get("60"))
        ));
    }

    private void createFillReport(
            String targetCompId,
            String clOrdId,
            String execSuffix,
            String symbol,
            String side,
            int quantity,
            BigDecimal price,
            LocalDateTime executedAt
    ) {
        String rawFix = builder.buildFillReport(targetCompId(targetCompId), clOrdId, nextExecId(execSuffix), symbol, side, quantity, price, quantity, executedAt);
        saveOutbound(clOrdId, rawFix, FixMessageStatus.SENT);
        producer.publishExecutionReport(clOrdId, rawFix);
        metricsService.recordExecutionReportSent();
    }

    private void createRejectedReport(String targetCompId, String clOrdId, String execId, String symbol, String side, String reason, LocalDateTime time) {
        String rawFix = builder.buildRejectedReport(targetCompId(targetCompId), clOrdId, execId, symbol, side, reason, time);
        saveOutbound(clOrdId, rawFix, FixMessageStatus.SENT);
        producer.publishExecutionReport(clOrdId, rawFix);
        metricsService.recordExecutionReportSent();
    }

    private void createCancelledReport(String targetCompId, String clOrdId, String execId, String symbol, String side, String reason, LocalDateTime time) {
        String rawFix = builder.buildCancelledReport(targetCompId(targetCompId), clOrdId, execId, symbol, side, reason, time);
        saveOutbound(clOrdId, rawFix, FixMessageStatus.SENT);
        producer.publishExecutionReport(clOrdId, rawFix);
        metricsService.recordExecutionReportSent();
    }

    private Map<String, String> sideForOrder(String clOrdId) {
        return repository.findFirstByDirectionAndClOrdIdOrderByCreatedAtDesc(FixDirection.INBOUND, clOrdId)
                .map(FixMessageEntity::getRawMessage)
                .map(parser::parse)
                .orElse(Map.of());
    }

    private FixMessageEntity saveInbound(
            String rawMessage,
            String messageType,
            String clOrdId,
            FixMessageStatus status,
            String rejectionReason
    ) {
        FixMessageEntity entity = new FixMessageEntity();
        entity.setFixMessageId(clOrdId == null ? UUID.randomUUID().toString() : clOrdId);
        entity.setDirection(FixDirection.INBOUND);
        entity.setMessageType(messageType);
        entity.setClOrdId(clOrdId);
        entity.setRawMessage(rawMessage);
        entity.setStatus(status);
        entity.setRejectionReason(rejectionReason);
        entity.setCreatedAt(LocalDateTime.now());
        return repository.save(entity);
    }

    private FixMessageEntity saveOutbound(String clOrdId, String rawFix, FixMessageStatus status) {
        FixMessageEntity entity = new FixMessageEntity();
        entity.setFixMessageId(nextExecId());
        entity.setDirection(FixDirection.OUTBOUND);
        entity.setMessageType("8");
        entity.setClOrdId(clOrdId);
        entity.setRawMessage(rawFix);
        entity.setStatus(status);
        entity.setCreatedAt(LocalDateTime.now());
        return repository.save(entity);
    }

    private FixExecutionReportResponse toReportResponse(FixMessageEntity entity) {
        return new FixExecutionReportResponse(entity.getClOrdId(), entity.getRawMessage(), entity.getCreatedAt());
    }

    private LocalDateTime parseFixTime(String value) {
        return LocalDateTime.parse(value, FIX_TIME);
    }

    private String toOrderSide(String fixSide) {
        return switch (fixSide) {
            case "1" -> "BUY";
            case "2" -> "SELL";
            default -> throw new IllegalArgumentException("Unsupported FIX side 54=" + fixSide);
        };
    }

    private String toFixSide(String orderSide) {
        if ("BUY".equalsIgnoreCase(orderSide)) {
            return "1";
        }
        if ("SELL".equalsIgnoreCase(orderSide)) {
            return "2";
        }
        return "1";
    }

    private String toOrderType(String fixOrderType) {
        return switch (fixOrderType) {
            case "1" -> "MARKET";
            case "2" -> "LIMIT";
            default -> throw new IllegalArgumentException("Unsupported FIX order type 40=" + fixOrderType);
        };
    }

    private String targetCompId(String targetCompId) {
        if (targetCompId == null || targetCompId.isBlank()) {
            return "UNKNOWN";
        }
        return targetCompId;
    }

    private String nextExecId() {
        return "EXEC-" + executionSequence.getAndIncrement();
    }

    private String nextExecId(String suffix) {
        return "EXEC-" + suffix + "-" + executionSequence.getAndIncrement();
    }
}
