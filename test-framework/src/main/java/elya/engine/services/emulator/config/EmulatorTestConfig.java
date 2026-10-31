package elya.engine.services.emulator.config;

import com.fasterxml.jackson.databind.*;
import elya.emulator.credentials.*;
import elya.engine.services.emulator.*;
import elya.restclient.api.*;
import elya.restclient.http.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.web.client.*;

/**
 * Main configuration class for the Emulator's test infrastructure.
 * <p>Declares the test-side bean graph explicitly: JSON processing, network engines,
 * high-level API facades and the emulator lifecycle manager. Nothing is component-scanned,
 * so beans of the emulator itself never end up in the test context.</p>
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

    /**
     * REST execution engine used by all API clients.
     * <p>The base URL is set by {@link EmulatorLifecycleManager} once the emulator has started.</p>
     *
     * @return a configured {@link RestClientApiEngine}.
     */
    @Bean
    public IRestClientApi testRestClientApiEngine() {
        return new RestClientApiEngine("");
    }

    /**
     * High-level API Facade that aggregates specialized clients.
     *
     * @param testRestClientApiEngine the underlying network engine.
     * @return a fully initialized {@link RestClientApi}.
     */
    @Bean
    public RestClientApi restClientApi(IRestClientApi testRestClientApiEngine) {
        return new RestClientApi(
                new AuthClient(testRestClientApiEngine),
                new BankCardClient(testRestClientApiEngine),
                new MockClient(testRestClientApiEngine)
        );
    }
}