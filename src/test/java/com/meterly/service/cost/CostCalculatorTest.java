package com.meterly.service.cost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.meterly.domain.Metric;
import com.meterly.domain.PeriodStatus;

/** Plain JUnit 5 unit test: no Spring, no database, runs in milliseconds. */
class CostCalculatorTest {

    private final CostCalculator calculator = new CostCalculator();

    private final Map<Metric, BigDecimal> prices = Map.of(
            Metric.API_CALLS, new BigDecimal("0.00020000"),
            Metric.AI_TOKENS, new BigDecimal("0.00001000"));

    private CostReport.Header header(String planIncluded) {
        return new CostReport.Header(1L, "Acme", new BigDecimal(planIncluded), 10L,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), PeriodStatus.OPEN);
    }

    @Test
    void rollsUpEndpointToServiceToAccountWithoutDoubleCounting() {
        List<UsageTotal> totals = List.of(
                new UsageTotal(1L, "search", 11L, "GET /a", Metric.API_CALLS, 10_000L),  // 2.00
                new UsageTotal(1L, "search", 11L, "GET /a", Metric.AI_TOKENS, 50_000L),  // 0.50
                new UsageTotal(1L, "search", 12L, "GET /b", Metric.API_CALLS, 5_000L),   // 1.00
                new UsageTotal(2L, "chat", 21L, "POST /c", Metric.AI_TOKENS, 300_000L)); // 3.00

        CostReport report = calculator.calculate(header("0"), totals, prices);

        assertThat(report.services()).hasSize(2);
        var search = report.services().get(0);
        assertThat(search.endpoints()).hasSize(2);
        assertThat(search.endpoints().get(0).subtotal()).isEqualByComparingTo("2.50");
        assertThat(search.subtotal()).isEqualByComparingTo("3.50");
        assertThat(report.services().get(1).subtotal()).isEqualByComparingTo("3.00");
        assertThat(report.usageTotal()).isEqualByComparingTo("6.50");   // not 6.50 + 3.50 + ... = no double count
        assertThat(report.amountDue()).isEqualTo(new BigDecimal("6.50"));
    }

    @Test
    void planAllowanceIsSubtractedAndNeverGoesNegative() {
        List<UsageTotal> totals = List.of(new UsageTotal(1L, "s", 11L, "e", Metric.API_CALLS, 10_000L)); // 2.00

        assertThat(calculator.calculate(header("1.50"), totals, prices).amountDue()).isEqualTo(new BigDecimal("0.50"));
        assertThat(calculator.calculate(header("5"), totals, prices).amountDue()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void roundsOnlyTheFinalAmountWithBankersRounding() {
        // 0.0002 * 12_345 = 2.469 exactly; tiny line amounts are kept exact, the total is rounded once.
        List<UsageTotal> totals = List.of(new UsageTotal(1L, "s", 11L, "e", Metric.API_CALLS, 12_345L));
        CostReport report = calculator.calculate(header("0"), totals, prices);

        assertThat(report.usageTotal()).isEqualByComparingTo("2.469");
        assertThat(report.amountDue()).isEqualTo(new BigDecimal("2.47"));
        assertThat(CostCalculator.amountDue(new BigDecimal("0.125"), BigDecimal.ZERO)).isEqualTo(new BigDecimal("0.12"));
        assertThat(CostCalculator.amountDue(new BigDecimal("0.135"), BigDecimal.ZERO)).isEqualTo(new BigDecimal("0.14"));
    }

    @Test
    void emptyPeriodCostsNothing() {
        CostReport report = calculator.calculate(header("0"), List.of(), prices);
        assertThat(report.services()).isEmpty();
        assertThat(report.amountDue()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void missingPriceIsAnError() {
        List<UsageTotal> totals = List.of(new UsageTotal(1L, "s", 11L, "e", Metric.EGRESS, 1L));
        assertThatThrownBy(() -> calculator.calculate(header("0"), totals, prices))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EGRESS");
    }
}
