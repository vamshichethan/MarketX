package com.marketx.oms.repository;

import com.marketx.oms.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeRepository extends JpaRepository<TradeEntity, Long> {
    Optional<TradeEntity> findByTradeId(String tradeId);
}
