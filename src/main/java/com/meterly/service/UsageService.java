package com.meterly.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meterly.api.ApiException;
import com.meterly.api.dto.UsageDtos.UsageRequest;
import com.meterly.domain.BillingPeriod;
import com.meterly.domain.Endpoint;
import com.meterly.domain.PeriodUsage;
import com.meterly.domain.UsageEvent;
import com.meterly.repository.BillingPeriodRepository;
import com.meterly.repository.EndpointRepository;
import com.meterly.repository.PeriodUsageRepository;
import com.meterly.repository.UsageEventRepository;

/** Stores one usage event inside a single database transaction. */
@Service
public class UsageService {

    private final UsageEventRepository usageEvents;
    private final EndpointRepository endpoints;
    private final BillingPeriodRepository periods;
    private final PeriodUsageRepository periodUsage;

    public UsageService(UsageEventRepository usageEvents, EndpointRepository endpoints,
                        BillingPeriodRepository periods, PeriodUsageRepository periodUsage) {
        this.usageEvents = usageEvents;
        this.endpoints = endpoints;
        this.periods = periods;
        this.periodUsage = periodUsage;
    }

    /** Result of recording: the event, and whether it was newly created or a replay of an earlier request. */
    public record Recorded(UsageEvent event, boolean created) {
    }

    @Transactional
    public Recorded record(String idempotencyKey, UsageRequest request) {
        // 1. Same key seen before? Return the stored event instead of counting it again.
        Optional<UsageEvent> existing = findExisting(idempotencyKey, request);
        if (existing.isPresent()) {
            return new Recorded(existing.get(), false);
        }

        // 2. Find where the usage belongs: endpoint -> service -> account -> its OPEN period.
        Endpoint endpoint = endpoints.findWithServiceAndAccount(request.endpointId())
                .orElseThrow(() -> ApiException.notFound("Endpoint", request.endpointId()));
        Long accountId = endpoint.getService().getAccount().getId();
        // FOR SHARE lock: the period can't be closed while this transaction is still running.
        BillingPeriod period = periods.findOpenForUsage(accountId)
                .orElseThrow(() -> ApiException.conflict("Account " + accountId + " has no open billing period"));

        // 3. Insert. saveAndFlush sends the INSERT now, so a duplicate key fails here, inside this method.
        Instant occurredAt = request.occurredAt() != null ? request.occurredAt() : Instant.now();
        UsageEvent event = new UsageEvent(endpoint, period, request.metric(), request.quantity(),
                idempotencyKey, occurredAt);
        UsageEvent saved = usageEvents.saveAndFlush(event);

        // 4. Add to the running total (Step 5). If another worker updated the same counter first,
        //    @Version makes the UPDATE fail and the whole transaction (event included) rolls back,
        //    so event and counter can never disagree. UsageIngestion then retries.
        PeriodUsage counter = periodUsage
                .findByBillingPeriodIdAndEndpointIdAndMetric(period.getId(), endpoint.getId(), request.metric())
                .orElseGet(() -> new PeriodUsage(period, endpoint, request.metric()));
        counter.add(request.quantity());
        periodUsage.saveAndFlush(counter);

        return new Recorded(saved, true);
    }

    /**
     * Looks up an earlier event with this key. If the key was reused for a DIFFERENT request,
     * that's a client bug: answer 422 instead of silently returning the old event.
     */
    @Transactional(readOnly = true)
    public Optional<UsageEvent> findExisting(String idempotencyKey, UsageRequest request) {
        Optional<UsageEvent> existing = usageEvents.findByIdempotencyKey(idempotencyKey);
        existing.ifPresent(e -> {
            boolean same = e.getEndpoint().getId().equals(request.endpointId())
                    && e.getMetric() == request.metric()
                    && e.getQuantity() == request.quantity();
            if (!same) {
                throw ApiException.unprocessable(
                        "Idempotency-Key '" + idempotencyKey + "' was already used for a different request");
            }
        });
        return existing;
    }
}
