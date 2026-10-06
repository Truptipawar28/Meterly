package com.meterly.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Running total of one metric on one endpoint in one billing period. A "hot" row under concurrency. */
@Entity
@Table(name = "period_usage")
public class PeriodUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_period_id", nullable = false)
    private BillingPeriod billingPeriod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Metric metric;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "event_count", nullable = false)
    private long eventCount;

    // UPDATE period_usage SET quantity=?, version=version+1 WHERE id=? AND version=?
    // 0 rows updated => someone else changed it first => ObjectOptimisticLockingFailureException.
    @Version
    @Column(nullable = false)
    private Long version;

    protected PeriodUsage() {
    }

    public PeriodUsage(BillingPeriod billingPeriod, Endpoint endpoint, Metric metric) {
        this.billingPeriod = billingPeriod;
        this.endpoint = endpoint;
        this.metric = metric;
    }

    public void add(long amount) {
        this.quantity += amount;
        this.eventCount++;
    }

    public Long getId() { return id; }
    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public Endpoint getEndpoint() { return endpoint; }
    public Metric getMetric() { return metric; }
    public long getQuantity() { return quantity; }
    public long getEventCount() { return eventCount; }
    public Long getVersion() { return version; }
}
