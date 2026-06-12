package com.marketx.risk.repository;

import com.marketx.risk.entity.RiskDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskDecisionRepository extends JpaRepository<RiskDecisionEntity, Long> {
    List<RiskDecisionEntity> findByAccountIdOrderByCheckedAtDesc(String accountId);
}
