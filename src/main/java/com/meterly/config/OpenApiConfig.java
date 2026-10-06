package com.meterly.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * springdoc scans the controllers and builds the OpenAPI spec at /v3/api-docs;
 * Swagger UI at /swagger-ui.html renders it and lets you try requests.
 * The "bearerAuth" scheme adds an "Authorize" button where you paste a JWT.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Meterly API", version = "v1",
                description = "Metered billing: usage ingestion, live cost, period close and invoices. "
                        + "Get a token from POST /api/auth/token (admin/admin123, worker/worker123, client/client123), "
                        + "then click Authorize."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
