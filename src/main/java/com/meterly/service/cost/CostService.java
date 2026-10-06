package com.meterly.service.cost;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meterly.api.ApiException;
import com.meterly.domain.Account;
import com.meterly.domain.BillingPeriod;
import com.meterly.domain.Metric;
import com.meterly.domain.MetricPrice;
import com.meterly.domain.PeriodStatus;
import com.meterly.domain.PeriodUsage;
import com.meterly.repository.BillingPeriodRepository;
import com.meterly.repository.MetricPriceRepository;
import com.meterly.repository.PeriodUsageRepository;
import com.meterly.repository.UsageEventRepository;

/** Loads the data for a cost report and hands it to the pure CostCalculator. */
@Service
public class CostService {

    private final BillingPeriodRepository periods;
    private final UsageEventRepository usageEvents;
    private final PeriodUsageRepository periodUsage;
    private final MetricPriceRepository prices;
    private final CostCalculator calculator;

    public CostService(BillingPeriodRepository periods, UsageEventRepository usageEvents,
                       PeriodUsageRepository periodUsage, MetricPriceRepository prices,
                       CostCalculator calculator) {
        this.periods = periods;
        this.usageEvents = usageEvents;
        this.periodUsage = periodUsage;
        this.prices = prices;
        this.calculator = calculator;
    }

    /** Live cost of the account's current OPEN period. */
    @Transactional(readOnly = true)
    public CostReport currentCost(Long accountId) {
        BillingPeriod period = periods.findByAccountIdAndStatus(accountId, PeriodStatus.OPEN)
                .orElseThrow(() -> ApiException.notFound("Open billing period for account", accountId));
        return report(period);
    }

    @Transactional(readOnly = true)
    public CostReport costForPeriod(Long periodId) {
        return report(getPeriod(periodId));
    }

    /** Must be called inside a transaction (it reads the lazy account). */
    public CostReport report(BillingPeriod period) {
        Account account = period.getAccount();
        var header = new CostReport.Header(account.getId(), account.getName(), account.getPlanIncluded(),
                period.getId(), period.getStartsOn(), period.getEndsOn(), period.getStatus());
        return calculator.calculate(header, usageEvents.sumByEndpointAndMetric(period.getId()), priceMap());
    }

    @Transactional(readOnly = true)
    public BillingPeriod getPeriod(Long periodId) {
        return periods.findById(periodId).orElseThrow(() -> ApiException.notFound("Billing period", periodId));
    }

    /** A difference between a running counter and the events behind it. */
    public record Mismatch(Long endpointId, Metric metric, long counterQuantity, long eventQuantity) {
    }

    /**
     * Reconciliation: compare the running totals (period_usage) with the sum of the raw events.
     * An empty list means they agree. Any mismatch points to a bug (e.g. a lost update or double count).
     */
    @Transactional(readOnly = true)
    public List<Mismatch> reconcile(Long periodId) {
        getPeriod(periodId);
        Map<String, Long> fromEvents = new HashMap<>();
        for (UsageTotal t : usageEvents.sumByEndpointAndMetric(periodId)) {
            fromEvents.put(t.endpointId() + "/" + t.metric(), t.quantity());
        }
        List<Mismatch> mismatches = new ArrayList<>();
        for (PeriodUsage c : periodUsage.findByBillingPeriodId(periodId)) {
            String key = c.getEndpoint().getId() + "/" + c.getMetric();
            long events = fromEvents.getOrDefault(key, 0L);
            if (events != c.getQuantity()) {
                mismatches.add(new Mismatch(c.getEndpoint().getId(), c.getMetric(), c.getQuantity(), events));
            }
            fromEvents.remove(key);
        }
        // Events with no counter row at all
        fromEvents.forEach((key, qty) -> {
            String[] parts = key.split("/");
            mismatches.add(new Mismatch(Long.valueOf(parts[0]), Metric.valueOf(parts[1]), 0, qty));
        });
        return mismatches;
    }

    private Map<Metric, BigDecimal> priceMap() {
        Map<Metric, BigDecimal> map = new HashMap<>();
        for (MetricPrice p : prices.findAll()) {
            map.put(p.getMetric(), p.getUnitPrice());
        }
        return map;
    }
}
