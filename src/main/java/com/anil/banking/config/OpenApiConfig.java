package com.anil.banking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at http://localhost:8080/swagger-ui.html. Log in via POST /api/auth/login,
 * copy the token, click "Authorize" and paste it to call the protected endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI bankingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking Transaction System API")
                        .version("2.0.0")
                        .description("Accounts, deposits, withdrawals and ACID-safe transfers, secured with JWT. "
                                + "Log in with POST /api/auth/login, then click Authorize and paste the token."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
