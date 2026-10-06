package com.meterly.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Who may call what.
 *  WRITER: internal services that log usage      -> POST /api/usage
 *  READER: a customer's dashboard (own account)  -> GET cost / invoices of THEIR account
 *  ADMIN : operators                             -> everything
 */
@Configuration
@EnableMethodSecurity   // enables @PreAuthorize on controller methods
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    public static final String ISSUER = "meterly";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No cookies/sessions: each request carries its own token, so CSRF protection isn't needed
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/api/auth/token", "/actuator/health/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usage").hasAnyRole("WRITER", "ADMIN")
                        .requestMatchers("/api/me").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/accounts/*", "/api/accounts/*/cost", "/api/accounts/*/invoices",
                                "/api/periods/*/cost", "/api/invoices/*").hasAnyRole("READER", "ADMIN")
                        .anyRequest().hasRole("ADMIN"))
                // Validate "Authorization: Bearer <jwt>" on every request
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromJwt())));
        return http.build();
    }

    /** The JWT claim "roles": ["ADMIN"] becomes the Spring authority ROLE_ADMIN (what hasRole("ADMIN") checks). */
    private JwtAuthenticationConverter rolesFromJwt() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** HS256: the same secret signs (encoder) and verifies (decoder) tokens. */
    @Bean
    SecretKey jwtSigningKey(SecurityProperties props) {
        byte[] bytes = props.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("meterly.security.jwt-secret must be at least 32 bytes for HS256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));   // checks exp, nbf and iss
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }
}
