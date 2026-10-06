package com.meterly.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.meterly.domain.Invoice;
import com.meterly.domain.InvoiceLine;
import com.meterly.domain.Metric;

public final class InvoiceDtos {

    private InvoiceDtos() {
    }

    public record InvoiceSummary(Long id, Long accountId, Long periodId, LocalDate startsOn, LocalDate endsOn,
                                 BigDecimal amountDue, Instant issuedAt) {
        public static InvoiceSummary from(Invoice i) {
            return new InvoiceSummary(i.getId(), i.getAccount().getId(), i.getBillingPeriod().getId(),
                    i.getBillingPeriod().getStartsOn(), i.getBillingPeriod().getEndsOn(),
                    i.getAmountDue(), i.getIssuedAt());
        }
    }

    public record InvoiceLineResponse(String serviceName, String endpointName, Metric metric, long quantity,
                                      BigDecimal unitPrice, BigDecimal amount) {
        static InvoiceLineResponse from(InvoiceLine l) {
            return new InvoiceLineResponse(l.getServiceName(), l.getEndpointName(), l.getMetric(), l.getQuantity(),
                    l.getUnitPrice(), l.getAmount());
        }
    }

    public record InvoiceResponse(Long id, Long accountId, Long periodId, LocalDate startsOn, LocalDate endsOn,
                                  List<InvoiceLineResponse> lines, BigDecimal usageTotal, BigDecimal planIncluded,
                                  BigDecimal amountDue, Instant issuedAt) {
        public static InvoiceResponse from(Invoice i) {
            return new InvoiceResponse(i.getId(), i.getAccount().getId(), i.getBillingPeriod().getId(),
                    i.getBillingPeriod().getStartsOn(), i.getBillingPeriod().getEndsOn(),
                    i.getLines().stream().map(InvoiceLineResponse::from).toList(),
                    i.getUsageTotal(), i.getPlanIncluded(), i.getAmountDue(), i.getIssuedAt());
        }
    }
}
