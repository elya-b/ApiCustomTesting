package elya.engine.services.emulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import elya.ApiEmulatorHttpStatusInfoGenerator;
import elya.api.*;
import elya.credentials.ApiEmulatorCredentialsService;
import elya.credentials.ApiEmulatorCredentialsStructure;
import elya.interfaces.IRestClientApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * Main configuration class for the Emulator's test infrastructure.
 * <p>Defines the bean graph required for integration testing, including
 * JSON processing, network engines, and high-level API facades.</p>
 */
@Configuration
@ComponentScan(basePackages = "elya")
@EnableConfigurationProperties(ApiEmulatorCredentialsStructure.class)
@Import(ApiEmulatorCredentialsService.class)
public class EmulatorTestConfig {

    /** Standard Spring RestTemplate for alternative or legacy HTTP calls. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /** Centralized Jackson mapper for consistent serialization across tests. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /** * Default base URL for the emulator.
     * Can be overridden by test properties or environment variables.
     */
    @Bean
    public String baseUrl() {
        return "";
    }

    /**
     * Primary REST execution engine.
     * <p>Marked as {@link Primary} to ensure this implementation is preferred
     * when injecting {@link IRestClientApi} into other components.</p>
     *
     * @param statusGenerator service for enriching responses with HTTP metadata.
     * @return a configured {@link RestClientApiEngine}.
     */
    @Bean
    @Primary
    public IRestClientApi testRestClientApiEngine(ApiEmulatorHttpStatusInfoGenerator statusGenerator) {
        return new RestClientApiEngine("", statusGenerator);
    }

    /**
     * High-level API Facade that aggregates specialized clients.
     * <p>Uses the established {@code testRestClientApiEngine} to provide a
     * simplified interface for tests to interact with the emulator.</p>
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