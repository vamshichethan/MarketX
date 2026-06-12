package com.marketx.position.repository;

import com.marketx.position.entity.ProcessedTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedTradeRepository extends JpaRepository<ProcessedTradeEntity, Long> {
    boolean existsByTradeId(String tradeId);
}
