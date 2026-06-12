package com.marketx.risk.service;

import com.marketx.risk.dto.RiskLimitRequest;
import com.marketx.risk.dto.RiskLimitResponse;
import com.marketx.risk.entity.RiskLimitEntity;
import com.marketx.risk.exception.InvalidRiskRequestException;
import com.marketx.risk.exception.RiskLimitNotFoundException;
import com.marketx.risk.repository.RiskLimitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RiskLimitService {
    private static final int DEFAULT_MAX_ORDER_QUANTITY = 10_000;
    private static final int DEFAULT_MAX_POSITION_QUANTITY = 50_000;
    private static final BigDecimal DEFAULT_MAX_EXPOSURE = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal DEFAULT_MAX_DAILY_LOSS = BigDecimal.valueOf(50_000);

    private final RiskLimitRepository riskLimitRepository;

    public RiskLimitService(RiskLimitRepository riskLimitRepository) {
        this.riskLimitRepository = riskLimitRepository;
    }

    @Transactional
    public RiskLimitResponse updateLimits(String accountId, RiskLimitRequest request) {
        validateAccount(accountId);
        validateRequest(request);

        String normalizedAccountId = accountId.toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        RiskLimitEntity limits = riskLimitRepository.findByAccountId(normalizedAccountId)
                .orElseGet(() -> {
                    RiskLimitEntity entity = new RiskLimitEntity();
                    entity.setAccountId(normalizedAccountId);
                    entity.setCreatedAt(now);
                    return entity;
                });

        limits.setMaxOrderQuantity(request.maxOrderQuantity());
        limits.setMaxPositionQuantity(request.maxPositionQuantity());
        limits.setMaxExposure(request.maxExposure());
        limits.setMaxDailyLoss(request.maxDailyLoss());
        limits.setUpdatedAt(now);

        return toResponse(riskLimitRepository.save(limits));
    }

    public RiskLimitResponse getLimits(String accountId) {
        validateAccount(accountId);
        return riskLimitRepository.findByAccountId(accountId.toUpperCase())
                .map(this::toResponse)
                .orElseThrow(() -> new RiskLimitNotFoundException(accountId));
    }

    @Transactional
    public RiskLimitEntity getOrCreateDefaults(String accountId) {
        String normalizedAccountId = accountId.toUpperCase();
        return riskLimitRepository.findByAccountId(normalizedAccountId)
                .orElseGet(() -> riskLimitRepository.save(defaultLimits(normalizedAccountId)));
    }

    private RiskLimitEntity defaultLimits(String accountId) {
        LocalDateTime now = LocalDateTime.now();
        RiskLimitEntity entity = new RiskLimitEntity();
        entity.setAccountId(accountId);
        entity.setMaxOrderQuantity(DEFAULT_MAX_ORDER_QUANTITY);
        entity.setMaxPositionQuantity(DEFAULT_MAX_POSITION_QUANTITY);
        entity.setMaxExposure(DEFAULT_MAX_EXPOSURE);
        entity.setMaxDailyLoss(DEFAULT_MAX_DAILY_LOSS);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private void validateRequest(RiskLimitRequest request) {
        if (request.maxOrderQuantity() <= 0) {
            throw new InvalidRiskRequestException("maxOrderQuantity must be greater than zero");
        }
        if (request.maxPositionQuantity() <= 0) {
            throw new InvalidRiskRequestException("maxPositionQuantity must be greater than zero");
        }
        if (request.maxExposure() == null || request.maxExposure().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRiskRequestException("maxExposure must be greater than zero");
        }
        if (request.maxDailyLoss() == null || request.maxDailyLoss().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRiskRequestException("maxDailyLoss must be greater than zero");
        }
    }

    private void validateAccount(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new InvalidRiskRequestException("accountId is required");
        }
    }

    private RiskLimitResponse toResponse(RiskLimitEntity entity) {
        return new RiskLimitResponse(
                entity.getAccountId(),
                entity.getMaxOrderQuantity(),
                entity.getMaxPositionQuantity(),
                entity.getMaxExposure(),
                entity.getMaxDailyLoss(),
                entity.getUpdatedAt()
        );
    }
}
