package com.meterly.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Price per unit of a metric. The rows are seeded by V1__init_schema.sql. */
@Entity
@Table(name = "metric_price")
public class MetricPrice {

    // The primary key is the metric name itself, not a generated number.
    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Metric metric;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal unitPrice;

    protected MetricPrice() {
    }

    public MetricPrice(Metric metric, BigDecimal unitPrice) {
        this.metric = metric;
        this.unitPrice = unitPrice;
    }

    public Metric getMetric() { return metric; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
