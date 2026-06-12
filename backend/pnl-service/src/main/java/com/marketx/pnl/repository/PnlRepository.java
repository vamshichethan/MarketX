package com.marketx.pnl.repository;

import com.marketx.pnl.entity.PnlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PnlRepository extends JpaRepository<PnlEntity, Long> {
    Optional<PnlEntity> findByAccountIdAndSymbol(String accountId, String symbol);

    List<PnlEntity> findByAccountId(String accountId);

    List<PnlEntity> findBySymbol(String symbol);
}
