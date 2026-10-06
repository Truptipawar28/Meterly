package com.meterly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.meterly.api.ApiException;
import com.meterly.api.dto.UsageDtos.UsageRequest;
import com.meterly.domain.Metric;
import com.meterly.domain.PeriodUsage;
import com.meterly.domain.UsageEvent;
import com.meterly.service.UsageService.Recorded;

/**
 * Unit test with Mockito: UsageService is replaced by a fake ("mock") whose behaviour we script,
 * so we can test the retry logic alone, without a database, and force rare race conditions on demand.
 */
@ExtendWith(MockitoExtension.class)
class UsageIngestionTest {

    @Mock UsageService usageService;

    private UsageIngestion ingestion;
    private final UsageRequest request = new UsageRequest(1L, Metric.API_CALLS, 5, null);

    @BeforeEach
    void setUp() {
        ingestion = new UsageIngestion(usageService, 3);   // max 3 attempts
    }

    @Test
    void retriesAfterAnOptimisticLockConflictAndThenSucceeds() {
        Recorded ok = new Recorded(mock(UsageEvent.class), true);
        when(usageService.record("k1", request))
                .thenThrow(new ObjectOptimisticLockingFailureException(PeriodUsage.class, 1L))  // 1st call: conflict
                .thenReturn(ok);                                                                // 2nd call: works

        assertThat(ingestion.ingest("k1", request)).isSameAs(ok);
        verify(usageService, times(2)).record("k1", request);
    }

    @Test
    void lostDuplicateKeyRaceReturnsTheWinnersEvent() {
        UsageEvent winner = mock(UsageEvent.class);
        when(usageService.record("k2", request)).thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(usageService.findExisting("k2", request)).thenReturn(Optional.of(winner));

        Recorded result = ingestion.ingest("k2", request);

        assertThat(result.created()).isFalse();
        assertThat(result.event()).isSameAs(winner);
        verify(usageService, times(1)).record(any(), any());   // no pointless retry
    }

    @Test
    void counterRowRaceIsRetried() {
        Recorded ok = new Recorded(mock(UsageEvent.class), true);
        when(usageService.record("k3", request))
                .thenThrow(new DataIntegrityViolationException("duplicate period_usage row"))
                .thenReturn(ok);
        when(usageService.findExisting("k3", request)).thenReturn(Optional.empty());

        assertThat(ingestion.ingest("k3", request)).isSameAs(ok);
    }

    @Test
    void givesUpWith409AfterMaxAttempts() {
        when(usageService.record(eq("k4"), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(PeriodUsage.class, 1L));

        assertThatThrownBy(() -> ingestion.ingest("k4", request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("retry");
        verify(usageService, times(3)).record("k4", request);
        verify(usageService, never()).findExisting(any(), any());
    }
}
