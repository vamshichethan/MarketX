package com.marketx.fixgateway.repository;

import com.marketx.fixgateway.entity.FixMessageEntity;
import com.marketx.fixgateway.enums.FixDirection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixMessageRepository extends JpaRepository<FixMessageEntity, Long> {
    List<FixMessageEntity> findByDirectionOrderByCreatedAtDesc(FixDirection direction);

    List<FixMessageEntity> findByDirectionAndClOrdIdOrderByCreatedAtDesc(FixDirection direction, String clOrdId);

    Optional<FixMessageEntity> findFirstByDirectionAndClOrdIdOrderByCreatedAtDesc(FixDirection direction, String clOrdId);
}
