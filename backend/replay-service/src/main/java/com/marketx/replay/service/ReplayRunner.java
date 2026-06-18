package com.marketx.replay.service;

import com.marketx.replay.dto.MarketDataEvent;
import com.marketx.replay.dto.ReplayTick;
import com.marketx.replay.entity.ReplaySessionEntity;
import com.marketx.replay.enums.ReplayStatus;
import com.marketx.replay.producer.ReplayMarketDataProducer;
import com.marketx.replay.repository.ReplaySessionRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ReplayRunner {
    private static final Logger log = LoggerFactory.getLogger(ReplayRunner.class);
    private static final String SOURCE = "HISTORICAL_REPLAY";

    private final ReplaySessionRepository sessionRepository;
    private final ReplayMarketDataProducer producer;
    private final ReplayMetricsService metricsService;
    private final ConcurrentMap<String, Boolean> workerRunning = new ConcurrentHashMap<>();

    public ReplayRunner(
            ReplaySessionRepository sessionRepository,
            ReplayMarketDataProducer producer,
            ReplayMetricsService metricsService
    ) {
        this.sessionRepository = sessionRepository;
        this.producer = producer;
        this.metricsService = metricsService;
    }

    public boolean isWorkerRunning(String sessionId) {
        return workerRunning.containsKey(sessionId);
    }

    public void run(String sessionId, List<ReplayTick> ticks) {
        if (workerRunning.putIfAbsent(sessionId, true) != null) {
            log.info("Replay worker already active for sessionId={}", sessionId);
            return;
        }

        metricsService.recordSessionStarted();
        try {
            replayLoop(sessionId, ticks);
        } finally {
            workerRunning.remove(sessionId);
            metricsService.recordSessionFinished();
        }
    }

    private void replayLoop(String sessionId, List<ReplayTick> ticks) {
        while (true) {
            ReplaySessionEntity session = findSession(sessionId);
            if (session.getStatus() != ReplayStatus.RUNNING) {
                log.info("Replay worker exiting sessionId={} status={}", sessionId, session.getStatus());
                return;
            }
            if (session.getCurrentIndex() >= ticks.size()) {
                complete(session);
                return;
            }

            int index = session.getCurrentIndex();
            waitForReplayGap(session, ticks, index);

            session = findSession(sessionId);
            if (session.getStatus() != ReplayStatus.RUNNING) {
                log.info("Replay worker paused/stopped before publish sessionId={} status={}", sessionId, session.getStatus());
                return;
            }

            ReplayTick current = ticks.get(index);
            ReplayTick previous = previousTick(ticks, index, current.symbol());
            publishTick(session, current, previous);
            advance(session);
        }
    }

    private void waitForReplayGap(ReplaySessionEntity session, List<ReplayTick> ticks, int index) {
        if (index == 0) {
            return;
        }

        long originalGapMs = Math.max(0, Duration.between(ticks.get(index - 1).timestamp(), ticks.get(index).timestamp()).toMillis());
        long adjustedGapMs = originalGapMs / Math.max(1, session.getSpeedMultiplier());
        if (adjustedGapMs <= 0) {
            return;
        }

        try {
            Thread.sleep(adjustedGapMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private ReplayTick previousTick(List<ReplayTick> ticks, int index, String symbol) {
        for (int i = index - 1; i >= 0; i--) {
            ReplayTick candidate = ticks.get(i);
            if (candidate.symbol().equals(symbol)) {
                return candidate;
            }
        }
        return null;
    }

    private void publishTick(ReplaySessionEntity session, ReplayTick tick, ReplayTick previous) {
        BigDecimal previousPrice = previous == null ? tick.price() : previous.price();
        BigDecimal change = tick.price().subtract(previousPrice);
        BigDecimal changePercent = previousPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : change.divide(previousPrice, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        MarketDataEvent event = new MarketDataEvent(
                "REPLAY-EVT-" + UUID.randomUUID(),
                tick.symbol(),
                tick.price(),
                previousPrice,
                change,
                changePercent,
                tick.volume(),
                tick.bidPrice(),
                tick.askPrice(),
                tick.spread(),
                tick.timestamp(),
                SOURCE,
                session.getSessionId()
        );

        Timer.Sample sample = metricsService.startPublish();
        producer.publish(event);
        metricsService.recordTickPublished(sample);
    }

    @Transactional
    protected void advance(ReplaySessionEntity session) {
        ReplaySessionEntity current = findSession(session.getSessionId());
        current.setCurrentIndex(current.getCurrentIndex() + 1);
        current.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(current);
    }

    @Transactional
    protected void complete(ReplaySessionEntity session) {
        session.setStatus(ReplayStatus.COMPLETED);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("Replay completed sessionId={}", session.getSessionId());
    }

    private ReplaySessionEntity findSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalStateException("Replay session missing during run: " + sessionId));
    }
}
