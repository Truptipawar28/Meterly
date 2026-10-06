package com.meterly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.meterly.IntegrationTest;
import com.meterly.api.ApiException;
import com.meterly.api.dto.UsageDtos.UsageRequest;
import com.meterly.domain.Account;
import com.meterly.domain.Endpoint;
import com.meterly.domain.Invoice;
import com.meterly.domain.Metric;
import com.meterly.domain.PeriodStatus;
import com.meterly.service.cost.CostService;

class InvoiceFlowTest extends IntegrationTest {

    @Autowired CatalogService catalog;
    @Autowired UsageIngestion ingestion;
    @Autowired InvoiceService invoices;
    @Autowired CostService costs;
    @Autowired JdbcTemplate jdbc;

    @Test
    void closingAPeriodIssuesAFrozenInvoiceAndOpensTheNextPeriod() {
        Account account = catalog.createAccount("Invoice " + UUID.randomUUID(), new BigDecimal("1.00"));
        var service = catalog.createService(account.getId(), "search");
        Endpoint endpoint = catalog.createEndpoint(service.getId(), "GET /search");

        ingest(endpoint, Metric.API_CALLS, 10_000);    // 2.00
        ingest(endpoint, Metric.AI_TOKENS, 50_000);    // 0.50

        Invoice invoice = invoices.closePeriod(account.getId());

        assertThat(invoice.getUsageTotal()).isEqualByComparingTo("2.50");
        assertThat(invoice.getAmountDue()).isEqualByComparingTo("1.50");   // 2.50 - 1.00 allowance
        assertThat(invoice.getLines()).hasSize(2);
        assertThat(invoice.getBillingPeriod().getStatus()).isEqualTo(PeriodStatus.CLOSED);

        // A new OPEN period starts the day after the old one ended
        var current = costs.currentCost(account.getId());
        assertThat(current.startsOn()).isEqualTo(invoice.getBillingPeriod().getEndsOn().plusDays(1));
        assertThat(current.amountDue()).isEqualByComparingTo("0");

        // New usage lands in the new period; the invoice is unaffected
        ingest(endpoint, Metric.API_CALLS, 5_000);
        assertThat(costs.currentCost(account.getId()).usageTotal()).isEqualByComparingTo("1.00");
        assertThat(invoices.getInvoice(invoice.getId()).getAmountDue()).isEqualByComparingTo("1.50");
    }

    @Test
    void invoicesCannotBeChangedEvenWithRawSql() {
        Account account = catalog.createAccount("Immutable " + UUID.randomUUID(), BigDecimal.ZERO);
        Invoice invoice = invoices.closePeriod(account.getId());

        assertThatThrownBy(() -> jdbc.update("update invoice set amount_due = 0 where id = ?", invoice.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.update("delete from invoice where id = ?", invoice.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("immutable");
    }

    @Test
    void usageForAnAccountWithoutOpenPeriodIsRejected() {
        Account account = catalog.createAccount("NoPeriod " + UUID.randomUUID(), BigDecimal.ZERO);
        var service = catalog.createService(account.getId(), "s");
        Endpoint endpoint = catalog.createEndpoint(service.getId(), "e");
        jdbc.update("update billing_period set status = 'CLOSED' where account_id = ?", account.getId());

        assertThatThrownBy(() -> ingest(endpoint, Metric.API_CALLS, 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no open billing period");
    }

    private void ingest(Endpoint endpoint, Metric metric, long quantity) {
        ingestion.ingest(UUID.randomUUID().toString(), new UsageRequest(endpoint.getId(), metric, quantity, null));
    }
}
