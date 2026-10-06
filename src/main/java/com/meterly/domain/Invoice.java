package com.meterly.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Immutable;

import com.meterly.service.cost.CostReport;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * A finalized invoice. @Immutable: Hibernate never issues an UPDATE for it.
 * There are no setters, and the database triggers in V3 reject UPDATE/DELETE as a last line of defence.
 */
@Entity
@Immutable
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_period_id", nullable = false, updatable = false)
    private BillingPeriod billingPeriod;

    @Column(name = "usage_total", nullable = false, precision = 19, scale = 8)
    private BigDecimal usageTotal;

    @Column(name = "plan_included", nullable = false, precision = 19, scale = 6)
    private BigDecimal planIncluded;

    @Column(name = "amount_due", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    // One invoice has many lines. cascade = PERSIST: saving the invoice also inserts its lines.
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.PERSIST)
    @OrderBy("id")
    private List<InvoiceLine> lines = new ArrayList<>();

    protected Invoice() {
    }

    /** Freeze a cost report into an invoice. The only way to create one. */
    public static Invoice issue(BillingPeriod period, CostReport report) {
        Invoice invoice = new Invoice();
        invoice.account = period.getAccount();
        invoice.billingPeriod = period;
        invoice.usageTotal = report.usageTotal();
        invoice.planIncluded = report.planIncluded();
        invoice.amountDue = report.amountDue();
        invoice.issuedAt = Instant.now();
        for (var service : report.services()) {
            for (var endpoint : service.endpoints()) {
                for (var line : endpoint.lines()) {
                    invoice.lines.add(new InvoiceLine(invoice, service.name(), endpoint.name(), line.metric(),
                            line.quantity(), line.unitPrice(), line.amount()));
                }
            }
        }
        return invoice;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public BigDecimal getUsageTotal() { return usageTotal; }
    public BigDecimal getPlanIncluded() { return planIncluded; }
    public BigDecimal getAmountDue() { return amountDue; }
    public Instant getIssuedAt() { return issuedAt; }
    public List<InvoiceLine> getLines() { return Collections.unmodifiableList(lines); }
}
