package com.meterly.domain;

import java.math.BigDecimal;

import org.hibernate.annotations.Immutable;

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

/** One line of an invoice. Names and price are copied (snapshot), not referenced. */
@Entity
@Immutable
@Table(name = "invoice_line")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "endpoint_name", nullable = false, length = 200)
    private String endpointName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Metric metric;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal amount;

    protected InvoiceLine() {
    }

    InvoiceLine(Invoice invoice, String serviceName, String endpointName, Metric metric,
                long quantity, BigDecimal unitPrice, BigDecimal amount) {
        this.invoice = invoice;
        this.serviceName = serviceName;
        this.endpointName = endpointName;
        this.metric = metric;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public String getServiceName() { return serviceName; }
    public String getEndpointName() { return endpointName; }
    public Metric getMetric() { return metric; }
    public long getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getAmount() { return amount; }
}
