package com.meterly.service;

import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import com.meterly.api.ApiException;
import com.meterly.api.dto.UsageDtos.UsageRequest;
import com.meterly.service.UsageService.Recorded;

/**
 * Entry point for recording usage. Deliberately NOT @Transactional: when UsageService's transaction
 * fails because of a concurrent writer, we retry in a NEW transaction.
 *
 * Retried failures:
 *  - OptimisticLockingFailureException: another worker updated the same running total first (@Version).
 *  - DataIntegrityViolationException: a concurrent insert won a UNIQUE race. Either the same
 *    Idempotency-Key (=> return the winner's event) or the first counter row for this endpoint/metric
 *    (=> retry; the row exists now).
 *  - PessimisticLockingFailureException: deadlock / lock timeout detected by PostgreSQL.
 */
@Service
public class UsageIngestion {

    private static final Logger log = LoggerFactory.getLogger(UsageIngestion.class);

    private final UsageService usageService;
    private final int maxAttempts;

    public UsageIngestion(UsageService usageService,
                          @Value("${meterly.usage.max-attempts:20}") int maxAttempts) {
        this.usageService = usageService;
        this.maxAttempts = maxAttempts;
    }

    public Recorded ingest(String idempotencyKey, UsageRequest request) {
        for (int attempt = 1; ; attempt++) {
            try {
                return usageService.record(idempotencyKey, request);
            } catch (DataIntegrityViolationException e) {
                var existing = usageService.findExisting(idempotencyKey, request);
                if (existing.isPresent()) {
                    return new Recorded(existing.get(), false);   // lost the race to an identical request
                }
                giveUpOrWait(attempt, e);
            } catch (OptimisticLockingFailureException | PessimisticLockingFailureException e) {
                giveUpOrWait(attempt, e);
            }
        }
    }

    private void giveUpOrWait(int attempt, RuntimeException cause) {
        if (attempt >= maxAttempts) {
            log.warn("Giving up after {} attempts", attempt, cause);
            throw ApiException.conflict("Too much concurrent activity, please retry");
        }
        // Back off a random little while so competing threads don't collide again in lockstep ("jitter").
        long sleepMs = ThreadLocalRandom.current().nextLong(1, 5L * attempt + 1);
        log.debug("Attempt {} failed ({}), retrying in {} ms", attempt, cause.getClass().getSimpleName(), sleepMs);
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw cause;
        }
    }
}
