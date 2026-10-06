package com.meterly.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.meterly.api.dto.UsageDtos.UsageRequest;
import com.meterly.service.UsageService.Recorded;

/**
 * Entry point for recording usage. Deliberately NOT @Transactional: it calls UsageService,
 * and if that transaction fails it can react and try something else in a NEW transaction.
 */
@Service
public class UsageIngestion {

    private final UsageService usageService;

    public UsageIngestion(UsageService usageService) {
        this.usageService = usageService;
    }

    public Recorded ingest(String idempotencyKey, UsageRequest request) {
        try {
            return usageService.record(idempotencyKey, request);
        } catch (DataIntegrityViolationException e) {
            // Two identical requests arrived at the same moment: both saw "key not used yet",
            // the database UNIQUE constraint let only one INSERT through. The loser returns the winner's event.
            return usageService.findExisting(idempotencyKey, request)
                    .map(existing -> new Recorded(existing, false))
                    .orElseThrow(() -> e);
        }
    }
}
