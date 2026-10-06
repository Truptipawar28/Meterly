package com.meterly.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.meterly.IntegrationTest;
import com.meterly.domain.Account;
import com.meterly.domain.Endpoint;
import com.meterly.security.TokenService;
import com.meterly.service.CatalogService;

/** Real HTTP calls against the running app (random port): security rules + idempotency over the wire. */
class SecurityAndApiTest extends IntegrationTest {

    @Autowired TestRestTemplate http;
    @Autowired TokenService tokens;
    @Autowired CatalogService catalog;

    @Test
    void requestsWithoutTokenAreRejected() {
        assertThat(http.getForEntity("/api/accounts", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.getForEntity("/", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);   // public
    }

    @Test
    void loginIssuesATokenForValidDemoUsersOnly() {
        var bad = http.postForEntity("/api/auth/token", Map.of("username", "admin", "password", "nope"), String.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var ok = http.postForEntity("/api/auth/token", Map.of("username", "admin", "password", "admin123"), Map.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) ok.getBody().get("accessToken");
        assertThat(call(HttpMethod.GET, "/api/accounts", token, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void writerCanRecordUsageButNotReadCosts() {
        Endpoint endpoint = newEndpoint();
        Long accountId = endpoint.getService().getAccount().getId();
        String writer = tokens.issue("worker", List.of("WRITER"), null).accessToken();
        Map<String, Object> body = Map.of("endpointId", endpoint.getId(), "metric", "API_CALLS", "quantity", 10);
        String key = UUID.randomUUID().toString();

        var first = call(HttpMethod.POST, "/api/usage", writer, body, String.class, key);
        var retry = call(HttpMethod.POST, "/api/usage", writer, body, String.class, key);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retry.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true");
        assertThat(call(HttpMethod.GET, "/api/accounts/" + accountId + "/cost", writer, null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingIdempotencyKeyIsABadRequest() {
        Endpoint endpoint = newEndpoint();
        String writer = tokens.issue("worker", List.of("WRITER"), null).accessToken();
        Map<String, Object> body = Map.of("endpointId", endpoint.getId(), "metric", "API_CALLS", "quantity", 10);

        assertThat(call(HttpMethod.POST, "/api/usage", writer, body, String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void readerSeesOnlyTheirOwnAccount() {
        Account mine = catalog.createAccount("Mine " + UUID.randomUUID(), BigDecimal.ZERO);
        Account other = catalog.createAccount("Other " + UUID.randomUUID(), BigDecimal.ZERO);
        String reader = tokens.issue("client", List.of("READER"), mine.getId()).accessToken();

        assertThat(call(HttpMethod.GET, "/api/accounts/" + mine.getId() + "/cost", reader, null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "/api/accounts/" + mine.getId() + "/invoices", reader, null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "/api/accounts/" + other.getId() + "/cost", reader, null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        // Admin-only operations
        assertThat(call(HttpMethod.POST, "/api/accounts/" + mine.getId() + "/close-period", reader, null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "/api/accounts", reader, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Endpoint newEndpoint() {
        Account account = catalog.createAccount("Api " + UUID.randomUUID(), BigDecimal.ZERO);
        var service = catalog.createService(account.getId(), "svc");
        return catalog.createEndpoint(service.getId(), "GET /x");
    }

    private <T> ResponseEntity<T> call(HttpMethod method, String url, String token, Object body, Class<T> type,
                                       String... idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (idempotencyKey.length > 0) {
            headers.set("Idempotency-Key", idempotencyKey[0]);
        }
        return http.exchange(url, method, new HttpEntity<>(body, headers), type);
    }
}
