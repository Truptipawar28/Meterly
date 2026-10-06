package com.meterly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.meterly.domain.BillingPeriod;
import com.meterly.domain.PeriodStatus;

import jakarta.persistence.LockModeType;

public interface BillingPeriodRepository extends JpaRepository<BillingPeriod, Long> {

    Optional<BillingPeriod> findByAccountIdAndStatus(Long accountId, PeriodStatus status);

    List<BillingPeriod> findByAccountIdOrderByStartsOnDesc(Long accountId);

    /**
     * Used while recording usage: SELECT ... FOR SHARE.
     * Many ingesters can hold a SHARE lock at once (no contention between them),
     * but closing the period (FOR UPDATE) has to wait until they finish, and vice versa.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from BillingPeriod p where p.account.id = :accountId and p.status = com.meterly.domain.PeriodStatus.OPEN")
    Optional<BillingPeriod> findOpenForUsage(Long accountId);

    /** Used when closing: SELECT ... FOR UPDATE. Exclusive; new usage waits until the close commits. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BillingPeriod p where p.account.id = :accountId and p.status = com.meterly.domain.PeriodStatus.OPEN")
    Optional<BillingPeriod> findOpenForClose(Long accountId);
}
