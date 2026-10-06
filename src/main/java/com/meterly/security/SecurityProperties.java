package com.meterly.security;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds all "meterly.security.*" settings from application.properties into one typed object.
 *
 * users = demo accounts for the DEV token endpoint. In production an identity provider
 * (Keycloak, Auth0, Okta, Cognito...) issues the tokens and this list doesn't exist.
 */
@ConfigurationProperties(prefix = "meterly.security")
public record SecurityProperties(String jwtSecret, Duration tokenTtl, Map<String, DemoUser> users) {

    public record DemoUser(String password, List<String> roles, Long accountId) {
    }
}
