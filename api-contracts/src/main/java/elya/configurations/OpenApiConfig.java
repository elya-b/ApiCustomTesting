package elya.configurations;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * Configuration class for OpenAPI (Swagger) documentation.
 * Defines global security schemes and UI metadata for the Bank API Emulator.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Customizes the OpenAPI definition to include global Bearer Authentication support.
     * <p>This configuration adds a "bearerAuth" security scheme to the Swagger UI,
     * allowing users to input their session token and automatically include it
     * in the {@code Authorization} header for all protected endpoints.</p>
     *
     * @return a configured {@link OpenAPI} instance with JWT/Bearer security requirements
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name(HttpHeaders.AUTHORIZATION)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}