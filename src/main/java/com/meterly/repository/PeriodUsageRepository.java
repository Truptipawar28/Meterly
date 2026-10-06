package com.meterly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meterly.domain.Metric;
import com.meterly.domain.PeriodUsage;

public interface PeriodUsageRepository extends JpaRepository<PeriodUsage, Long> {

    Optional<PeriodUsage> findByBillingPeriodIdAndEndpointIdAndMetric(Long billingPeriodId, Long endpointId,
                                                                      Metric metric);

    List<PeriodUsage> findByBillingPeriodId(Long billingPeriodId);
}
