package com.meterly.api.dto;

import java.time.Instant;

import com.meterly.domain.Metric;
import com.meterly.domain.UsageEvent;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class UsageDtos {

    private UsageDtos() {
    }

    /** Body of POST /api/usage. occurredAt is optional (defaults to "now"). */
    public record UsageRequest(
            @NotNull Long endpointId,
            @NotNull Metric metric,
            @Positive long quantity,
            Instant occurredAt) {
    }

    public record UsageResponse(Long id, Long endpointId, Long billingPeriodId, Metric metric, long quantity,
                                String idempotencyKey, Instant occurredAt, Instant receivedAt) {
        public static UsageResponse from(UsageEvent e) {
            return new UsageResponse(e.getId(), e.getEndpoint().getId(), e.getBillingPeriod().getId(),
                    e.getMetric(), e.getQuantity(), e.getIdempotencyKey(), e.getOccurredAt(), e.getReceivedAt());
        }
    }
}
