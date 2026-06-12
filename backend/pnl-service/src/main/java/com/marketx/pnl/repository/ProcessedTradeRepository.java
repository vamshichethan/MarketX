package com.marketx.pnl.repository;

import com.marketx.pnl.entity.ProcessedTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedTradeRepository extends JpaRepository<ProcessedTradeEntity, Long> {
    boolean existsByTradeId(String tradeId);
}
