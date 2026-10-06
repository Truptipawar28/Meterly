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
    public ResponseEntity<UsageResponse> record(@RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
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
