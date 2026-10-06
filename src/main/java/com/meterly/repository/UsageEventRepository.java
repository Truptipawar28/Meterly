package com.meterly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.meterly.domain.UsageEvent;
import com.meterly.service.cost.UsageTotal;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

    Optional<UsageEvent> findByIdempotencyKey(String idempotencyKey);

    /**
     * Sum the raw events (the source of truth) per endpoint and metric, in one SQL GROUP BY.
     * The database does the heavy lifting; Java only multiplies by prices and rolls up.
     */
    @Query("""
            select new com.meterly.service.cost.UsageTotal(
                       s.id, s.name, e.id, e.name, u.metric, sum(u.quantity))
            from UsageEvent u
            join u.endpoint e
            join e.service s
            where u.billingPeriod.id = :periodId
            group by s.id, s.name, e.id, e.name, u.metric
            order by s.name, e.name, u.metric
            """)
    List<UsageTotal> sumByEndpointAndMetric(Long periodId);
}
