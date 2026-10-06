package com.meterly.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meterly.domain.Metric;
import com.meterly.domain.MetricPrice;

public interface MetricPriceRepository extends JpaRepository<MetricPrice, Metric> {
}
