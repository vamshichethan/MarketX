package com.marketx.risk.repository;

import com.marketx.risk.entity.MarketPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketPriceRepository extends JpaRepository<MarketPriceEntity, Long> {
    Optional<MarketPriceEntity> findBySymbol(String symbol);
}
