package elya.engine.services.emulator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.ApiEmulatorRunner;
import elya.api.RestClientApiEngine;
import elya.card.BankCard;
import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardRequest;
import elya.dto.bankcard.BankCardResponse;
import elya.interfaces.IRestClientApi;
import elya.repository.MockRepository;
import elya.repository.SessionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static elya.constants.ApiEndpoints.*;
import static elya.constants.enums.HttpHeaderValues.BEARER;
import static elya.engine.services.emulator.constants.exceptions.Exceptions.*;
import static elya.engine.services.emulator.constants.logs.InfoLogs.EMULATOR_IS_READY;
import static elya.engine.services.emulator.constants.logs.InfoLogs.MANAGER_START;
import static elya.enums.responsemodel.ApiBankCards.CARDS;
import static elya.enums.responsemodel.ApiBankCards.RESPONSE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Orchestrator for managing the API Emulator lifecycle within the testing framework.
 * <p>This manager is responsible for:
 * <ul>
 * <li>Starting and stopping the emulator process via {@link ApiEmulatorRunner}.</li>
 * <li>Monitoring the emulator health status using Spring Boot Actuator.</li>
 * <li>Handling initial authentication and session token storage.</li>
 * <li>Providing high-level methods for data seeding (mocking bank cards).</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class EmulatorLifecycleManager {

    /**
     * The base URL of the running emulator instance (e.g., http://localhost:8080).
     * Populated dynamically after the runner starts.
     */
    @Getter
    private String url;

    /**
     * The authentication token obtained during the startup sequence.
     * Used for subsequent authorized mock configurations.
     */
    @Getter
    private String authToken;

    private final ApiEmulatorRunner runner;
    private final IRestClientApi restClient;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    /**
     * Constructs the manager and binds it to a fixed port runner.
     *
     * @param restClient   low-level client for API interactions.
     * @param objectMapper Jackson mapper for data conversion.
     * @param restTemplate template for health check polling.
     */
    public EmulatorLifecycleManager(IRestClientApi restClient,
                                    ObjectMapper objectMapper,
                                    RestTemplate restTemplate) {
        this.runner = new ApiEmulatorRunner(8080);
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * Lifecycle callback executed after dependency injection.
     * Logs the manager initialization.
     */
    @PostConstruct
    public void init() {
        log.info(MANAGER_START);
    }

    /**
     * Triggers the full emulator startup sequence.
     * <ol>
     * <li>Starts the process.</li>
     * <li>Configures the base URL for the rest client.</li>
     * <li>Polls the health endpoint until "UP".</li>
     * <li>Authenticates and stores the token.</li>
     * </ol>
     *
     * @param authRequest credentials for initial login.
     * @throws RuntimeException if startup, health check, or login fails.
     */
    public void start(AuthRequest authRequest) {
        runner.start();
        int port = runner.getActualPort();
        this.url = "http://localhost:" + port;

        if (restClient instanceof RestClientApiEngine engine) {
            engine.setBaseUrl(this.url);
        }
        waitUntilReady(Duration.ofSeconds(30));

        runner.getBean(SessionRepository.class).clear();
        runner.getBean(MockRepository.class).clearAll();

        this.authToken = performLogin(authRequest);
        log.info("Emulator startup complete. Service is healthy and authenticated.");
    }

    /**
     * Gracefully shuts down the emulator process.
     * Invoked automatically by the Spring container on shutdown.
     */
    @PreDestroy
    public void stop() {
        if (runner.isRunning()) {
            runner.getBean(SessionRepository.class).clear();
            runner.getBean(MockRepository.class).clearAll();
        }
        runner.stop();
        log.info("Emulator process terminated.");
    }

    /**
     * Internal method to perform authentication against the newly started emulator.
     *
     * @param authRequest user credentials.
     * @return a valid JWT or session token.
     * @throws IllegalStateException if the token cannot be generated.
     */
    private String performLogin(AuthRequest authRequest) {
        AuthResponse response;
        try {
            JsonNode body = objectMapper.valueToTree(authRequest);
            JsonNode responseNode = restClient.post(this.url + URL_TOKEN, body, Map.of());
            log.info("performLogin response: {}", responseNode);
            response = objectMapper.treeToValue(responseNode, AuthResponse.class);
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
            throw new RuntimeException("Critical mapping error during token generation", e);
        }

        if (response != null && response.getSuccess() && response.getData() != null) {
            return response.getData().getToken();
        } else {
            throw new IllegalStateException(FAILED_TO_GENERATE_AUTH_TOKEN);
        }
    }

    /**
     * Seeds the emulator with a predefined list of bank cards for mocking.
     * <p>Translates domain {@link BankCard} entities into DTOs and performs an authorized POST.</p>
     *
     * @param token authentication token.
     * @param cards domain cards to be mocked.
     * @return the list of cards as confirmed by the emulator.
     */
    public List<BankCard> addBankCards(String token, List<BankCard> cards) {
        List<BankCardRequest> dtos = cards.stream()
                .map(BankCardRequest::fromDomain)
                .collect(Collectors.toList());

        Map<String, String> headers = Map.of(AUTHORIZATION, BEARER + token);
        JsonNode responseNode = restClient.post(url + URL_BANK_CARD_DATA,
                objectMapper.valueToTree(BankCardListRequest.of(dtos)), headers);

        JsonNode cardsNode = responseNode.path(RESPONSE.toString()).path(CARDS.toString());
        List<BankCardResponse> resultDtos = objectMapper.convertValue(cardsNode, new TypeReference<>() {});

        return resultDtos.stream()
                .map(BankCardResponse::getBankCard)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the current list of cards from the emulator.
     *
     * @param token authentication token.
     * @return a list of {@link BankCard} objects.
     */
    public List<BankCard> getBankCards(String token) {
        try {
            Map<String, String> headers = Map.of(AUTHORIZATION, BEARER + token);
            JsonNode responseNode = restClient.get(url + URL_BANK_CARD_DATA, headers);

            JsonNode cardsNode = responseNode.path(RESPONSE.toString()).path(CARDS.toString());
            if (cardsNode.isMissingNode()) return List.of();

            List<BankCardResponse> dtos = objectMapper.convertValue(cardsNode, new TypeReference<>() {});
            return dtos.stream()
                    .map(BankCardResponse::getBankCard)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to retrieve bank cards from {}", URL_BANK_CARD_DATA, e);
            return List.of();
        }
    }

    /**
     * Actuator-based polling logic.
     * <p>Wait for the service to return HTTP 200 on the /health endpoint.
     * Uses exponential backoff (fixed 500ms) until timeout.</p>
     *
     * @param timeout max duration to wait.
     */
    private void waitUntilReady(Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        String healthUrl = url + "/actuator/health";

        while (Instant.now().isBefore(deadline)) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info(EMULATOR_IS_READY);
                    return;
                }
            } catch (Exception ignored) {
                // Service not yet available, continue polling
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(INTERRUPTED_EMULATOR_STARTUP, e);
            }
        }
        throw new RuntimeException(FAILED_TO_START_EMULATOR + timeout.toSeconds() + " seconds");
    }
}