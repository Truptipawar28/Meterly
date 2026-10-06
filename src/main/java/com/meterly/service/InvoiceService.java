package com.meterly.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meterly.api.ApiException;
import com.meterly.domain.BillingPeriod;
import com.meterly.domain.Invoice;
import com.meterly.repository.BillingPeriodRepository;
import com.meterly.repository.InvoiceRepository;
import com.meterly.service.cost.CostReport;
import com.meterly.service.cost.CostService;

@Service
public class InvoiceService {

    private final BillingPeriodRepository periods;
    private final InvoiceRepository invoices;
    private final CostService costs;

    public InvoiceService(BillingPeriodRepository periods, InvoiceRepository invoices, CostService costs) {
        this.periods = periods;
        this.invoices = invoices;
        this.costs = costs;
    }

    /**
     * Close the account's open period and issue its invoice, all in ONE transaction:
     * either everything happens (period CLOSED + invoice + next period OPEN) or nothing does.
     */
    @Transactional
    public Invoice closePeriod(Long accountId) {
        // 1. FOR UPDATE: waits for in-flight usage transactions (they hold FOR SHARE), blocks new ones.
        BillingPeriod period = periods.findOpenForClose(accountId)
                .orElseThrow(() -> ApiException.conflict("Account " + accountId + " has no open billing period"));

        // 2. Compute the final cost. No new events can sneak in now.
        CostReport report = costs.report(period);

        // 3. Mark CLOSED and flush NOW. Hibernate normally runs INSERTs before UPDATEs at commit;
        //    the new OPEN period would then clash with the "one open period per account" index.
        period.close();
        periods.saveAndFlush(period);   // @Version is checked and incremented here

        // 4. Freeze the invoice, then open the next month.
        Invoice invoice = invoices.save(Invoice.issue(period, report));
        LocalDate nextStart = period.getEndsOn().plusDays(1);
        periods.save(new BillingPeriod(period.getAccount(), nextStart, YearMonth.from(nextStart).atEndOfMonth()));
        return invoice;
    }

    @Transactional(readOnly = true)
    public Invoice getInvoice(Long invoiceId) {
        return invoices.findWithLines(invoiceId).orElseThrow(() -> ApiException.notFound("Invoice", invoiceId));
    }

    @Transactional(readOnly = true)
    public List<Invoice> listInvoices(Long accountId) {
        return invoices.findByAccount(accountId);
    }
}
