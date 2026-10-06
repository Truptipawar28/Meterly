package com.meterly.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Row-level rule: a READER may only see the account in their token's "account_id" claim.
 * Named "access" so @PreAuthorize can call it: @PreAuthorize("@access.canRead(authentication, #accountId)").
 */
@Component("access")
public class AccountAccess {

    public boolean canRead(Authentication auth, Long accountId) {
        if (auth == null) {
            return false;
        }
        if (hasRole(auth, "ROLE_ADMIN")) {
            return true;
        }
        return hasRole(auth, "ROLE_READER") && accountId != null && accountId.equals(accountIdOf(auth));
    }

    /** For endpoints where the account is only known after loading the data (invoice, period). */
    public void check(Long accountId) {
        if (!canRead(SecurityContextHolder.getContext().getAuthentication(), accountId)) {
            throw new AccessDeniedException("No access to account " + accountId);
        }
    }

    public static Long accountIdOf(Authentication auth) {
        if (auth.getPrincipal() instanceof Jwt jwt && jwt.getClaim("account_id") instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private static boolean hasRole(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }
}
