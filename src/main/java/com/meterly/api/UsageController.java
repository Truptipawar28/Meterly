package com.meterly.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meterly.api.dto.UsageDtos.UsageRequest;
import com.meterly.api.dto.UsageDtos.UsageResponse;
import com.meterly.service.UsageIngestion;
import com.meterly.service.UsageService.Recorded;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usage")
public class UsageController {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final UsageIngestion ingestion;

    public UsageController(UsageIngestion ingestion) {
        this.ingestion = ingestion;
    }

    /**
     * Record usage. The client must send a unique Idempotency-Key header per logical event.
     * 201 Created = stored now; 200 OK + "Idempotent-Replayed: true" = already stored earlier, not counted again.
     */
    @PostMapping
    @Operation(summary = "Record usage (role WRITER)",
            description = "201 = stored. 200 + Idempotent-Replayed: true = same key seen before, not counted again. "
                    + "422 = key reused with a different body.")
    public ResponseEntity<UsageResponse> record(
            @Parameter(description = "Unique per logical event; reuse it when retrying", example = "evt-2026-10-07-0001")
            @RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
                                                @Valid @RequestBody UsageRequest request) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key must be 1-100 characters");
        }
        Recorded result = ingestion.ingest(idempotencyKey, request);
        UsageResponse body = UsageResponse.from(result.event());
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
        return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(body);
    }
}
