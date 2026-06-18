package com.marketx.replay.service;

import com.marketx.replay.dto.CreateReplaySessionRequest;
import com.marketx.replay.dto.ReplaySessionResponse;
import com.marketx.replay.dto.ReplayTick;
import com.marketx.replay.entity.ReplaySessionEntity;
import com.marketx.replay.enums.ReplayStatus;
import com.marketx.replay.exception.InvalidReplayStateException;
import com.marketx.replay.exception.ReplaySessionNotFoundException;
import com.marketx.replay.repository.ReplaySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ReplayService {
    private static final Logger log = LoggerFactory.getLogger(ReplayService.class);

    private final ReplaySessionRepository sessionRepository;
    private final CsvReplayLoader csvReplayLoader;
    private final ReplayRunner replayRunner;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, List<ReplayTick>> sessionTicks = new ConcurrentHashMap<>();

    public ReplayService(
            ReplaySessionRepository sessionRepository,
            CsvReplayLoader csvReplayLoader,
            ReplayRunner replayRunner
    ) {
        this.sessionRepository = sessionRepository;
        this.csvReplayLoader = csvReplayLoader;
        this.replayRunner = replayRunner;
    }

    @Transactional
    public ReplaySessionResponse createSession(CreateReplaySessionRequest request) {
        int speed = normalizeSpeed(request.speedMultiplier());
        Path path = resolvePath(request.filePath());
        List<ReplayTick> ticks = csvReplayLoader.load(path);
        if (ticks.isEmpty()) {
            throw new InvalidReplayStateException("Replay CSV contains no valid ticks: " + path);
        }

        LocalDateTime now = LocalDateTime.now();
        ReplaySessionEntity session = new ReplaySessionEntity();
        session.setSessionId(nextSessionId());
        session.setFileName(path.toString());
        session.setStatus(ReplayStatus.CREATED);
        session.setSpeedMultiplier(speed);
        session.setCurrentIndex(0);
        session.setTotalTicks(ticks.size());
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        ReplaySessionEntity saved = sessionRepository.save(session);
        sessionTicks.put(saved.getSessionId(), ticks);
        log.info("Created replay sessionId={} file={} ticks={}", saved.getSessionId(), path, ticks.size());
        return toResponse(saved);
    }

    @Transactional
    public ReplaySessionResponse play(String sessionId) {
        ReplaySessionEntity session = getEntity(sessionId);
        if (session.getStatus() == ReplayStatus.RUNNING) {
            return toResponse(session);
        }
        if (session.getStatus() == ReplayStatus.COMPLETED) {
            throw new InvalidReplayStateException("Completed replay sessions cannot be resumed. Create a new session.");
        }

        List<ReplayTick> ticks = ticksFor(session);
        session.setStatus(ReplayStatus.RUNNING);
        session.setUpdatedAt(LocalDateTime.now());
        ReplaySessionEntity saved = sessionRepository.save(session);
        if (!replayRunner.isWorkerRunning(sessionId)) {
            executorService.submit(() -> replayRunner.run(sessionId, ticks));
        }
        log.info("Replay started sessionId={} speed={}x index={}", sessionId, saved.getSpeedMultiplier(), saved.getCurrentIndex());
        return toResponse(saved);
    }

    @Transactional
    public ReplaySessionResponse pause(String sessionId) {
        ReplaySessionEntity session = getEntity(sessionId);
        if (session.getStatus() == ReplayStatus.RUNNING) {
            session.setStatus(ReplayStatus.PAUSED);
            session.setUpdatedAt(LocalDateTime.now());
            session = sessionRepository.save(session);
            log.info("Replay paused sessionId={} index={}", sessionId, session.getCurrentIndex());
        }
        return toResponse(session);
    }

    @Transactional
    public ReplaySessionResponse stop(String sessionId) {
        ReplaySessionEntity session = getEntity(sessionId);
        session.setStatus(ReplayStatus.STOPPED);
        session.setCurrentIndex(0);
        session.setUpdatedAt(LocalDateTime.now());
        ReplaySessionEntity saved = sessionRepository.save(session);
        log.info("Replay stopped sessionId={}", sessionId);
        return toResponse(saved);
    }

    @Transactional
    public ReplaySessionResponse updateSpeed(String sessionId, int speedMultiplier) {
        ReplaySessionEntity session = getEntity(sessionId);
        session.setSpeedMultiplier(normalizeSpeed(speedMultiplier));
        session.setUpdatedAt(LocalDateTime.now());
        ReplaySessionEntity saved = sessionRepository.save(session);
        log.info("Replay speed changed sessionId={} speed={}x", sessionId, saved.getSpeedMultiplier());
        return toResponse(saved);
    }

    public ReplaySessionResponse getSession(String sessionId) {
        return toResponse(getEntity(sessionId));
    }

    public List<ReplaySessionResponse> getSessions() {
        return sessionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private ReplaySessionEntity getEntity(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ReplaySessionNotFoundException(sessionId));
    }

    private List<ReplayTick> ticksFor(ReplaySessionEntity session) {
        return sessionTicks.computeIfAbsent(session.getSessionId(), ignored -> csvReplayLoader.load(resolvePath(session.getFileName())));
    }

    private int normalizeSpeed(int speedMultiplier) {
        return speedMultiplier <= 0 ? 1 : speedMultiplier;
    }

    private String nextSessionId() {
        return "REPLAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Path resolvePath(String filePath) {
        Path path = Path.of(filePath);
        if (path.isAbsolute() || path.toFile().exists()) {
            return path.normalize();
        }
        Path serviceRelative = Path.of("backend", "replay-service", filePath);
        if (serviceRelative.toFile().exists()) {
            return serviceRelative.normalize();
        }
        return path.normalize();
    }

    private ReplaySessionResponse toResponse(ReplaySessionEntity entity) {
        return new ReplaySessionResponse(
                entity.getSessionId(),
                entity.getFileName(),
                entity.getStatus(),
                entity.getSpeedMultiplier(),
                entity.getCurrentIndex(),
                entity.getTotalTicks(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
