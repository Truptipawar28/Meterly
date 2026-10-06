package com.meterly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meterly.domain.BillingPeriod;
import com.meterly.domain.PeriodStatus;

public interface BillingPeriodRepository extends JpaRepository<BillingPeriod, Long> {

    Optional<BillingPeriod> findByAccountIdAndStatus(Long accountId, PeriodStatus status);

    List<BillingPeriod> findByAccountIdOrderByStartsOnDesc(Long accountId);
}
