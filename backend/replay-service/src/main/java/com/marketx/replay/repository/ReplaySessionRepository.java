package com.marketx.replay.repository;

import com.marketx.replay.entity.ReplaySessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReplaySessionRepository extends JpaRepository<ReplaySessionEntity, Long> {
    Optional<ReplaySessionEntity> findBySessionId(String sessionId);
}
