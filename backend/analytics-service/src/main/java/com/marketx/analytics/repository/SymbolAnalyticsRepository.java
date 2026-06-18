package com.marketx.analytics.repository;

import com.marketx.analytics.entity.SymbolAnalyticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SymbolAnalyticsRepository extends JpaRepository<SymbolAnalyticsEntity, Long> {
    Optional<SymbolAnalyticsEntity> findBySymbol(String symbol);

    List<SymbolAnalyticsEntity> findAllByOrderBySymbolAsc();

    void deleteBySymbol(String symbol);
}
