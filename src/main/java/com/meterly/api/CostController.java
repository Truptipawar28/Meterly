package com.meterly.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meterly.service.cost.CostReport;
import com.meterly.service.cost.CostService;
import com.meterly.service.cost.CostService.Mismatch;

@RestController
@RequestMapping("/api")
public class CostController {

    private final CostService costs;

    public CostController(CostService costs) {
        this.costs = costs;
    }

    /** Live cost of the current open period: account -> services -> endpoints -> metrics. */
    @GetMapping("/accounts/{accountId}/cost")
    public CostReport currentCost(@PathVariable Long accountId) {
        return costs.currentCost(accountId);
    }

    @GetMapping("/periods/{periodId}/cost")
    public CostReport periodCost(@PathVariable Long periodId) {
        return costs.costForPeriod(periodId);
    }

    /** Ops check: running counters vs raw events. Empty list = all good. */
    @GetMapping("/periods/{periodId}/reconcile")
    public List<Mismatch> reconcile(@PathVariable Long periodId) {
        return costs.reconcile(periodId);
    }
}
