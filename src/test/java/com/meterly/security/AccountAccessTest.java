package com.meterly.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The row-level access rule, tested with hand-made authentication objects. */
class AccountAccessTest {

    private final AccountAccess access = new AccountAccess();

    private JwtAuthenticationToken auth(String role, Long accountId) {
        Jwt.Builder jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("u")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (accountId != null) {
            jwt.claim("account_id", accountId);
        }
        return new JwtAuthenticationToken(jwt.build(), List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test
    void adminCanReadAnyAccount() {
        assertThat(access.canRead(auth("ADMIN", null), 42L)).isTrue();
    }

    @Test
    void readerCanReadOnlyTheAccountInTheToken() {
        assertThat(access.canRead(auth("READER", 7L), 7L)).isTrue();
        assertThat(access.canRead(auth("READER", 7L), 8L)).isFalse();
        assertThat(access.canRead(auth("READER", null), 7L)).isFalse();
    }

    @Test
    void writerCannotReadAccounts() {
        assertThat(access.canRead(auth("WRITER", 7L), 7L)).isFalse();
        assertThat(access.canRead(null, 7L)).isFalse();
    }
}
