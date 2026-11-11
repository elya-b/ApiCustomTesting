package elya.engine.services.emulator.config;

import com.fasterxml.jackson.databind.*;
import elya.emulator.credentials.*;
import elya.engine.services.emulator.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.web.client.*;

/**
 * Main configuration class for the Emulator's test infrastructure.
 * <p>Declares the test-side bean graph explicitly: JSON processing, the health-check template,
 * the shared users and the emulator lifecycle manager. Nothing is component-scanned,
 * so beans of the emulator itself never end up in the test context.</p>
 * <p>Every bean here is stateless or read-only and safe to share between parallel tests.
 * Anything bound to one emulator, such as its URL or its client, lives in the
 * {@link EmulatorSession} that {@link EmulatorLifecycleManager} creates for each test.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApiEmulatorCredentialsStructure.class)
@Import({EmulatorLifecycleManager.class, ApiEmulatorCredentialsService.class})
public class EmulatorTestConfig {

    /** Standard Spring RestTemplate used for health check polling. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /** Centralized Jackson mapper for consistent serialization across tests. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
