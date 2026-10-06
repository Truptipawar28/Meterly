package com.meterly.service.cost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.meterly.domain.Metric;
import com.meterly.domain.PeriodStatus;

/** Cost of one billing period, as a tree: account -> services -> endpoints -> metric lines. */
public record CostReport(
        Long accountId,
        String accountName,
        Long periodId,
        LocalDate startsOn,
        LocalDate endsOn,
        PeriodStatus status,
        List<ServiceCost> services,
        BigDecimal usageTotal,      // exact sum of all lines
        BigDecimal planIncluded,    // free allowance
        BigDecimal amountDue) {     // max(0, usageTotal - planIncluded), rounded to cents

    public record ServiceCost(Long serviceId, String name, List<EndpointCost> endpoints, BigDecimal subtotal) {
    }

    public record EndpointCost(Long endpointId, String name, List<LineCost> lines, BigDecimal subtotal) {
    }

    public record LineCost(Metric metric, long quantity, BigDecimal unitPrice, BigDecimal amount) {
    }

    /** Header data the calculator needs about the account and period. */
    public record Header(Long accountId, String accountName, BigDecimal planIncluded,
                         Long periodId, LocalDate startsOn, LocalDate endsOn, PeriodStatus status) {
    }
}
