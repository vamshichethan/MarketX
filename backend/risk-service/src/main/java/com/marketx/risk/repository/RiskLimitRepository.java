package com.marketx.risk.repository;

import com.marketx.risk.entity.RiskLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiskLimitRepository extends JpaRepository<RiskLimitEntity, Long> {
    Optional<RiskLimitEntity> findByAccountId(String accountId);
}
