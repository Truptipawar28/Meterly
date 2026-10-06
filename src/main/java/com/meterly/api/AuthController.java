package com.meterly.api;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meterly.security.AccountAccess;
import com.meterly.security.TokenService;
import com.meterly.security.TokenService.TokenResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final TokenService tokens;

    public AuthController(TokenService tokens) {
        this.tokens = tokens;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /** DEV ONLY: exchange a demo username/password for a JWT. */
    @PostMapping("/auth/token")
    public TokenResponse token(@Valid @RequestBody LoginRequest request) {
        return tokens.login(request.username(), request.password());
    }

    /** Who am I? Reads the already-validated token. */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replaceFirst("^ROLE_", ""))
                .toList();
        Long accountId = AccountAccess.accountIdOf(auth);
        return accountId == null
                ? Map.of("username", auth.getName(), "roles", roles)
                : Map.of("username", auth.getName(), "roles", roles, "accountId", accountId);
    }
}
