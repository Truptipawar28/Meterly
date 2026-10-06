package com.meterly.config;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.meterly.api.dto.UsageDtos.UsageRequest;
import com.meterly.domain.Account;
import com.meterly.domain.Endpoint;
import com.meterly.domain.Metric;
import com.meterly.repository.AccountRepository;
import com.meterly.service.CatalogService;
import com.meterly.service.InvoiceService;
import com.meterly.service.UsageIngestion;

/**
 * Fills an EMPTY database with sample data so the API and portal have something to show:
 * "Acme Corp" (account 1, used by the demo "client" login) with last month invoiced and usage this month.
 * Runs once at startup; does nothing if any account exists. Turn off with meterly.demo-data.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "meterly.demo-data.enabled", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final AccountRepository accounts;
    private final CatalogService catalog;
    private final UsageIngestion ingestion;
    private final InvoiceService invoices;

    public DemoDataLoader(AccountRepository accounts, CatalogService catalog, UsageIngestion ingestion,
                          InvoiceService invoices) {
        this.accounts = accounts;
        this.catalog = catalog;
        this.ingestion = ingestion;
        this.invoices = invoices;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accounts.count() > 0) {
            return;
        }
        YearMonth lastMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(1);

        Account acme = catalog.createAccount("Acme Corp", new BigDecimal("5.00"), lastMonth);
        var search = catalog.createService(acme.getId(), "search-api");
        Endpoint query = catalog.createEndpoint(search.getId(), "GET /search");
        Endpoint suggest = catalog.createEndpoint(search.getId(), "GET /suggest");
        var ai = catalog.createService(acme.getId(), "ai-assistant");
        Endpoint chat = catalog.createEndpoint(ai.getId(), "POST /chat");
        Endpoint files = catalog.createEndpoint(ai.getId(), "GET /files");

        // Last month: usage, then close -> invoice, and this month's period opens
        record("acme-last-1", query, Metric.API_CALLS, 42_000);
        record("acme-last-2", suggest, Metric.API_CALLS, 18_500);
        record("acme-last-3", chat, Metric.AI_TOKENS, 1_250_000);
        record("acme-last-4", files, Metric.EGRESS, 20_000);
        invoices.closePeriod(acme.getId());

        // This month so far
        record("acme-now-1", query, Metric.API_CALLS, 23_000);
        record("acme-now-2", suggest, Metric.API_CALLS, 9_100);
        record("acme-now-3", chat, Metric.AI_TOKENS, 640_000);
        record("acme-now-4", files, Metric.STORAGE, 150_000);
        record("acme-now-5", files, Metric.EGRESS, 7_500);

        Account globex = catalog.createAccount("Globex", BigDecimal.ZERO);
        var billing = catalog.createService(globex.getId(), "billing");
        Endpoint charge = catalog.createEndpoint(billing.getId(), "POST /charge");
        record("globex-now-1", charge, Metric.API_CALLS, 3_000);

        log.info("Demo data created: accounts {} (Acme Corp) and {} (Globex)", acme.getId(), globex.getId());
    }

    private void record(String key, Endpoint endpoint, Metric metric, long quantity) {
        ingestion.ingest(key, new UsageRequest(endpoint.getId(), metric, quantity, null));
    }
}
