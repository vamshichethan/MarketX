package com.marketx.pnl.repository;

import com.marketx.pnl.entity.MarketPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketPriceRepository extends JpaRepository<MarketPriceEntity, Long> {
    Optional<MarketPriceEntity> findBySymbol(String symbol);
}
