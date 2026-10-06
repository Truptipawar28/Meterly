package com.meterly.service.cost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.meterly.domain.Metric;
import com.meterly.service.cost.CostReport.EndpointCost;
import com.meterly.service.cost.CostReport.Header;
import com.meterly.service.cost.CostReport.LineCost;
import com.meterly.service.cost.CostReport.ServiceCost;

/**
 * Pure cost math: no database, no Spring magic, so it's easy to unit test.
 *
 * Rule against double counting: money is computed ONLY at the leaf (endpoint + metric line).
 * Endpoint, service and account totals are sums of the level below, never recomputed from
 * events again, so each event contributes to every level exactly once.
 */
@Component
public class CostCalculator {

    /** Invoices are in whole cents; intermediate amounts keep full precision. */
    public static final int MONEY_SCALE = 2;

    public CostReport calculate(Header header, List<UsageTotal> totals, Map<Metric, BigDecimal> prices) {
        // Group the flat rows: service -> endpoint -> lines. LinkedHashMap keeps the query's sort order.
        Map<Long, String> serviceNames = new LinkedHashMap<>();
        Map<Long, Map<Long, String>> endpointsByService = new LinkedHashMap<>();
        Map<Long, List<LineCost>> linesByEndpoint = new LinkedHashMap<>();

        for (UsageTotal t : totals) {
            BigDecimal unitPrice = prices.get(t.metric());
            if (unitPrice == null) {
                throw new IllegalStateException("No price configured for metric " + t.metric());
            }
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(t.quantity()));
            serviceNames.putIfAbsent(t.serviceId(), t.serviceName());
            endpointsByService.computeIfAbsent(t.serviceId(), id -> new LinkedHashMap<>())
                    .putIfAbsent(t.endpointId(), t.endpointName());
            linesByEndpoint.computeIfAbsent(t.endpointId(), id -> new ArrayList<>())
                    .add(new LineCost(t.metric(), t.quantity(), unitPrice, amount));
        }

        // Roll up: lines -> endpoint subtotal -> service subtotal -> account total.
        List<ServiceCost> services = new ArrayList<>();
        BigDecimal usageTotal = BigDecimal.ZERO;
        for (var service : serviceNames.entrySet()) {
            List<EndpointCost> endpoints = new ArrayList<>();
            BigDecimal serviceSubtotal = BigDecimal.ZERO;
            for (var endpoint : endpointsByService.get(service.getKey()).entrySet()) {
                List<LineCost> lines = linesByEndpoint.get(endpoint.getKey());
                BigDecimal endpointSubtotal = sum(lines.stream().map(LineCost::amount).toList());
                endpoints.add(new EndpointCost(endpoint.getKey(), endpoint.getValue(), lines, endpointSubtotal));
                serviceSubtotal = serviceSubtotal.add(endpointSubtotal);
            }
            services.add(new ServiceCost(service.getKey(), service.getValue(), endpoints, serviceSubtotal));
            usageTotal = usageTotal.add(serviceSubtotal);
        }

        return new CostReport(header.accountId(), header.accountName(), header.periodId(),
                header.startsOn(), header.endsOn(), header.status(), services,
                usageTotal, header.planIncluded(), amountDue(usageTotal, header.planIncluded()));
    }

    /** What the customer pays: usage above the free allowance, never negative, rounded once at the end. */
    static BigDecimal amountDue(BigDecimal usageTotal, BigDecimal planIncluded) {
        BigDecimal due = usageTotal.subtract(planIncluded);
        if (due.signum() < 0) {
            due = BigDecimal.ZERO;
        }
        // HALF_EVEN ("banker's rounding") avoids a systematic upward bias over many invoices
        return due.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
