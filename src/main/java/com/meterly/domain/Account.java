package com.meterly.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A customer. Top of the hierarchy: account -> service -> endpoint. */
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // BIGSERIAL: the database generates the id
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    // Free allowance included in the plan. Money is always BigDecimal, never double.
    @Column(name = "plan_included", nullable = false, precision = 19, scale = 6)
    private BigDecimal planIncluded = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Account() {
        // required by JPA
    }

    public Account(String name, BigDecimal planIncluded) {
        this.name = name;
        this.planIncluded = planIncluded;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPlanIncluded() { return planIncluded; }
    public Instant getCreatedAt() { return createdAt; }
}
