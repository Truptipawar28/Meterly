package com.meterly.domain;

import java.time.LocalDate;

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

/** One billing period (normally a month) for an account. Only one can be OPEN at a time. */
@Entity
@Table(name = "billing_period")
public class BillingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    // Store the enum as its name ("OPEN"), not its position (0). Positions break if the enum is reordered.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeriodStatus status = PeriodStatus.OPEN;

    // Optimistic locking: Hibernate adds "WHERE version = ?" to every UPDATE and bumps it.
    // If someone else changed the row first, the update fails instead of silently overwriting (Step 5).
    @Version
    @Column(nullable = false)
    private Long version;

    protected BillingPeriod() {
    }

    public BillingPeriod(Account account, LocalDate startsOn, LocalDate endsOn) {
        this.account = account;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }

    public void close() {
        this.status = PeriodStatus.CLOSED;
    }

    public boolean isOpen() { return status == PeriodStatus.OPEN; }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public LocalDate getStartsOn() { return startsOn; }
    public LocalDate getEndsOn() { return endsOn; }
    public PeriodStatus getStatus() { return status; }
    public Long getVersion() { return version; }
}
