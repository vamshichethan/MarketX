package com.marketx.position.repository;

import com.marketx.position.entity.PositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<PositionEntity, Long> {
    Optional<PositionEntity> findByAccountIdAndSymbol(String accountId, String symbol);

    List<PositionEntity> findByAccountId(String accountId);
}
