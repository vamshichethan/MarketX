package com.marketx.oms.repository;

import com.marketx.oms.entity.ExecutionReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExecutionReportRepository extends JpaRepository<ExecutionReportEntity, Long> {
    List<ExecutionReportEntity> findByOrderId(String orderId);

    Optional<ExecutionReportEntity> findByExecutionId(String executionId);
}
