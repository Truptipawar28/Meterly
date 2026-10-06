package com.meterly.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.meterly.api.ApiException;

/** DEV token issuer. Real deployments get tokens from an identity provider instead. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final SecurityProperties props;

    public TokenService(JwtEncoder encoder, SecurityProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn,
                                List<String> roles, Long accountId) {
    }

    public TokenResponse login(String username, String password) {
        SecurityProperties.DemoUser user = props.users().get(username);
        // Constant-time comparison: doesn't leak how many characters matched through response timing
        if (user == null || password == null || !MessageDigest.isEqual(
                user.password().getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return issue(username, user.roles(), user.accountId());
    }

    /** Build and sign a JWT. Public so tests can mint tokens for any role/account. */
    public TokenResponse issue(String subject, List<String> roles, Long accountId) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(SecurityConfig.ISSUER)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(props.tokenTtl()))
                .claim("roles", roles);
        if (accountId != null) {
            claims.claim("account_id", accountId);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new TokenResponse(token, "Bearer", props.tokenTtl().toSeconds(), roles, accountId);
    }
}
