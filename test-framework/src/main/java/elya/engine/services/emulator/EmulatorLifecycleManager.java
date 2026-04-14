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
 * It utilizes {@link ApiEmulatorRunner} for startup and handles high-level initialization
 * such as health monitoring and initial authentication.
 */
@Slf4j
@Component
public class EmulatorLifecycleManager {
    /**
     * The base URL of the running emulator instance (e.g., http://localhost:8080).
     */
    @Getter
    private String url;

    /**
     * The authentication token obtained during the startup sequence.
     */
    @Getter
    private String authToken;

    private final ApiEmulatorRunner runner;
    private final IRestClientApi restClient;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    /**
     * Initializes the manager with a runner set to use a random port (0).
     *
     * @param restClient   The implementation of {@link IRestClientApi} for HTTP interactions.
     * @param objectMapper The Jackson mapper for DTO serialization/deserialization.
     */
    public EmulatorLifecycleManager(
                                        IRestClientApi restClient,
                                        ObjectMapper objectMapper,
                                        RestTemplate restTemplate) {
        this.runner = new ApiEmulatorRunner(8080);
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * Performs post-construction logging of the manager's configuration.
     */
    @PostConstruct
    public void init() {
        log.info(MANAGER_START);
    }

    /**
     * Orchestrates the full startup sequence: triggers the technical runner,
     * resolves dynamic URL, waits for the service to be healthy, and performs
     * initial authentication to store a valid session token.
     *
     * @param authRequest The object containing credentials (login and password)
     * for automatic token generation during startup.
     */
    public void start(AuthRequest authRequest) {
        runner.start();
        int port = runner.getActualPort();
        this.url = "http://localhost:" + port;

        if (restClient instanceof RestClientApiEngine engine) {
            engine.setBaseUrl(this.url);
        }

        waitUntilReady(Duration.ofSeconds(30));

        this.authToken = performLogin(authRequest);
        log.info("Emulator started and authenticated. Token obtained.");
    }

    /**
     * Gracefully terminates the emulator instance by delegating to the runner.
     */
    @PreDestroy
    public void stop() {
        runner.stop();
    }

    /**
     * Obtains a valid session token by performing a REST login request.
     *
     * @param authRequest The object containing user credentials.
     * @return String representing the generated authentication token.
     * @throws IllegalStateException if the authentication fails or the response structure is invalid.
     * @throws RuntimeException      if an error occurs during JSON serialization or mapping.
     */
    private String performLogin(AuthRequest authRequest) {
        AuthResponse response;
        try {
            JsonNode body = objectMapper.valueToTree(authRequest);
            JsonNode responseNode = restClient.post(this.url + URL_TOKEN, body, Map.of());
            response = objectMapper.treeToValue(responseNode, AuthResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Mapping error during token generation", e);
        }

        if (response != null && response.getSuccess() && response.getData() != null) {
            return response.getData().getToken();
        } else {
            throw new IllegalStateException(FAILED_TO_GENERATE_AUTH_TOKEN);
        }
    }

    public List<BankCard> addBankCards(String token, List<BankCard> cards) {
        List<BankCardRequest> dtos = cards.stream()
                .map(BankCardRequest::fromDomain)
                .collect(Collectors.toList());
        BankCardListRequest requestBody = BankCardListRequest.of(dtos);

        Map<String, String> headers = Map.of(AUTHORIZATION, BEARER + token);
        JsonNode responseNode = restClient.post(url + URL_BANK_CARD_MOCK, objectMapper.valueToTree(requestBody), headers);

        log.info("Emulator seeded successfully. Response: {}", responseNode.toPrettyString());

        JsonNode cardsNode = responseNode.path(RESPONSE.toString()).path(CARDS.toString());
        List<BankCardResponse> resultDtos = objectMapper.convertValue(cardsNode, new TypeReference<>() {});

        return resultDtos.stream()
                .map(BankCardResponse::getBankCard)
                .collect(Collectors.toList());
    }

    /**
     * Fetches bank cards from the emulator and converts them into the internal domain model.
     *
     * @param token The authorization token.
     * @return A list of {@link BankCard} domain objects.
     */
    public List<BankCard> getBankCards(String token) {
        try {
            Map<String, String> headers = Map.of(AUTHORIZATION, BEARER + token);
            JsonNode responseNode = restClient.get(url + URL_BANK_CARD_DATA, headers);

            if (responseNode == null || responseNode.isMissingNode()) {
                return List.of();
            }

            var dtos = objectMapper.readValue(
                    responseNode.traverse(),
                    new TypeReference<List<BankCardResponse>>() {}
            );
            return dtos.stream()
                    .map(BankCardResponse::getBankCard)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to retrieve bank cards from {}", URL_BANK_CARD_DATA, e);
            return List.of();
        }
    }

    /**
     * Polls the Emulator's health actuator until it returns an OK status or the timeout is reached.
     * Ensures that the application is fully initialized before tests begin execution.
     *
     * @param timeout The maximum duration to wait for the application to become ready.
     * @throws RuntimeException if the emulator fails to start or is interrupted.
     */
    private void waitUntilReady(Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        String healthUrl = url + "/actuator/health";
        while (Instant.now().isBefore(deadline)) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl,
                        String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info(EMULATOR_IS_READY);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(INTERRUPTED_EMULATOR_STARTUP, e);
            }
        }
        throw new RuntimeException(FAILED_TO_START_EMULATOR + timeout);
    }
}
