package com.meterly.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.meterly.domain.Account;
import com.meterly.domain.BillingPeriod;
import com.meterly.domain.Endpoint;
import com.meterly.domain.PeriodStatus;
import com.meterly.domain.ServiceEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request/response shapes for the catalog API (accounts, services, endpoints).
 * Records = small immutable data classes; Jackson converts them to/from JSON.
 * We never return entities directly, so the API shape doesn't change when the database does.
 */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CreateAccountRequest(
            @NotBlank @Size(max = 200) String name,
            @PositiveOrZero BigDecimal planIncluded) {
    }

    public record AccountResponse(Long id, String name, BigDecimal planIncluded, Instant createdAt) {
        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getId(), a.getName(), a.getPlanIncluded(), a.getCreatedAt());
        }
    }

    public record CreateNamedRequest(@NotBlank @Size(max = 200) String name) {
    }

    public record ServiceResponse(Long id, Long accountId, String name) {
        public static ServiceResponse from(ServiceEntity s) {
            return new ServiceResponse(s.getId(), s.getAccount().getId(), s.getName());
        }
    }

    public record EndpointResponse(Long id, Long serviceId, String name) {
        public static EndpointResponse from(Endpoint e) {
            return new EndpointResponse(e.getId(), e.getService().getId(), e.getName());
        }
    }

    public record PeriodResponse(Long id, Long accountId, LocalDate startsOn, LocalDate endsOn,
                                 PeriodStatus status, Long version) {
        public static PeriodResponse from(BillingPeriod p) {
            return new PeriodResponse(p.getId(), p.getAccount().getId(), p.getStartsOn(), p.getEndsOn(),
                    p.getStatus(), p.getVersion());
        }
    }
}
