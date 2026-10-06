package com.meterly.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meterly.security.AccountAccess;
import com.meterly.service.cost.CostReport;
import com.meterly.service.cost.CostService;
import com.meterly.service.cost.CostService.Mismatch;

@RestController
@RequestMapping("/api")
public class CostController {

    private final CostService costs;
    private final AccountAccess access;

    public CostController(CostService costs, AccountAccess access) {
        this.costs = costs;
        this.access = access;
    }

    /** Live cost of the current open period: account -> services -> endpoints -> metrics. */
    @GetMapping("/accounts/{accountId}/cost")
    @PreAuthorize("@access.canRead(authentication, #accountId)")
    public CostReport currentCost(@PathVariable Long accountId) {
        return costs.currentCost(accountId);
    }

    @GetMapping("/periods/{periodId}/cost")
    public CostReport periodCost(@PathVariable Long periodId) {
        CostReport report = costs.costForPeriod(periodId);
        access.check(report.accountId());   // the account is only known after loading the period
        return report;
    }

    /** Ops check: running counters vs raw events. Empty list = all good. (ADMIN only, see SecurityConfig) */
    @GetMapping("/periods/{periodId}/reconcile")
    public List<Mismatch> reconcile(@PathVariable Long periodId) {
        return costs.reconcile(periodId);
    }
}
