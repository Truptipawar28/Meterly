package com.meterly.domain;

import java.time.Instant;

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

/** One raw usage report from a worker. Events are the source of truth for cost. */
@Entity
@Table(name = "usage_event")
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_period_id", nullable = false)
    private BillingPeriod billingPeriod;

    // Column "metric" is a foreign key to metric_price; we only need the value, so it's mapped as the enum.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Metric metric;

    @Column(nullable = false)
    private long quantity;

    // UNIQUE in the database: the same key sent twice is stored once (Step 4).
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt = Instant.now();

    protected UsageEvent() {
    }

    public UsageEvent(Endpoint endpoint, BillingPeriod billingPeriod, Metric metric,
                      long quantity, String idempotencyKey, Instant occurredAt) {
        this.endpoint = endpoint;
        this.billingPeriod = billingPeriod;
        this.metric = metric;
        this.quantity = quantity;
        this.idempotencyKey = idempotencyKey;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public Endpoint getEndpoint() { return endpoint; }
    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public Metric getMetric() { return metric; }
    public long getQuantity() { return quantity; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getReceivedAt() { return receivedAt; }
}
