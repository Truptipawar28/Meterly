package com.meterly.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meterly.domain.BillingPeriod;

public interface BillingPeriodRepository extends JpaRepository<BillingPeriod, Long> {
}
