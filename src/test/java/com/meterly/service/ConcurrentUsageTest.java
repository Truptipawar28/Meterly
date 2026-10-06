package com.meterly.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.meterly.IntegrationTest;
import com.meterly.api.dto.UsageDtos.UsageRequest;
import com.meterly.domain.Account;
import com.meterly.domain.Endpoint;
import com.meterly.domain.Metric;
import com.meterly.service.UsageService.Recorded;
import com.meterly.service.cost.CostService;

/** Many threads hammer the SAME counter row. Totals must still be exact. */
class ConcurrentUsageTest extends IntegrationTest {

    @Autowired CatalogService catalog;
    @Autowired UsageIngestion ingestion;
    @Autowired JdbcTemplate jdbc;
    @Autowired CostService costs;

    @Test
    void parallelWorkersAreCountedExactlyOnce() throws Exception {
        Endpoint endpoint = newEndpoint();
        int threads = 8;
        int eventsPerThread = 25;
        long quantity = 3;

        // Each thread sends its events TWICE (a retry storm). Keys are unique per event, shared by its retry.
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            List<String> keys = new ArrayList<>();
            for (int i = 0; i < eventsPerThread; i++) {
                keys.add(UUID.randomUUID().toString());
            }
            tasks.add(() -> {
                for (String key : keys) {
                    UsageRequest req = new UsageRequest(endpoint.getId(), Metric.API_CALLS, quantity, null);
                    ingestion.ingest(key, req);
                    ingestion.ingest(key, req);   // the retry: must not count again
                }
                return null;
            });
        }
        runAllAtOnce(tasks);

        long expectedEvents = (long) threads * eventsPerThread;
        Long events = jdbc.queryForObject(
                "select count(*) from usage_event where endpoint_id = ?", Long.class, endpoint.getId());
        var counter = jdbc.queryForMap(
                "select quantity, event_count, version from period_usage where endpoint_id = ?", endpoint.getId());

        assertThat(events).isEqualTo(expectedEvents);
        assertThat(counter.get("quantity")).isEqualTo(expectedEvents * quantity);
        assertThat(counter.get("event_count")).isEqualTo(expectedEvents);
        // version goes up once per successful update after the insert
        assertThat((Long) counter.get("version")).isEqualTo(expectedEvents - 1);

        // Step 6: running counters and raw events agree
        Long periodId = jdbc.queryForObject(
                "select billing_period_id from period_usage where endpoint_id = ?", Long.class, endpoint.getId());
        assertThat(costs.reconcile(periodId)).isEmpty();
    }

    @Test
    void sameKeyAtTheSameInstantIsStoredOnce() throws Exception {
        Endpoint endpoint = newEndpoint();
        String key = UUID.randomUUID().toString();
        UsageRequest req = new UsageRequest(endpoint.getId(), Metric.AI_TOKENS, 500, null);

        List<Callable<Recorded>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> ingestion.ingest(key, req));
        }
        List<Recorded> results = runAllAtOnce(tasks);

        assertThat(results).filteredOn(Recorded::created).hasSize(1);
        assertThat(results).extracting(r -> r.event().getId()).containsOnly(results.get(0).event().getId());
        assertThat(jdbc.queryForObject("select count(*) from usage_event where idempotency_key = ?",
                Long.class, key)).isEqualTo(1L);
    }

    private Endpoint newEndpoint() {
        Account account = catalog.createAccount("Concurrent " + UUID.randomUUID(), BigDecimal.ZERO);
        var service = catalog.createService(account.getId(), "svc");
        return catalog.createEndpoint(service.getId(), "GET /x");
    }

    /** Starts every task at the same moment (latch) to maximise collisions, then waits for all results. */
    private static <T> List<T> runAllAtOnce(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
