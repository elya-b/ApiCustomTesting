package elya.engine.services.emulator;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import elya.dto.auth.*;
import elya.emulator.*;
import elya.emulator.config.*;
import elya.restclient.http.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.client.*;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import static elya.constants.ApiEndpoints.*;
import static elya.engine.services.emulator.constants.exceptions.Exceptions.*;
import static elya.engine.services.emulator.constants.logs.InfoLogs.*;

/**
 * Starts emulators for the integration tests, one {@link EmulatorSession} per test.
 * <p>The manager holds no state of its own: every call to {@link #start(AuthRequest)} creates
 * a new emulator on a free port, with its own storage directory and its own client. Tests can
 * therefore start sessions in parallel without affecting each other.</p>
 * <p>Startup sequence:</p>
 * <ol>
 *   <li>start the emulator on a free port with a storage directory of its own;</li>
 *   <li>poll {@code /actuator/health} until the emulator answers;</li>
 *   <li>log in and keep the session token.</li>
 * </ol>
 * <p>If a step after the start fails, the emulator is stopped before the exception is rethrown,
 * so a failed setup does not leave a running server behind.</p>
 */
@Slf4j
@Component
public class EmulatorLifecycleManager {

    /** How long a new emulator may take to report itself healthy. */
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);

    /** Pause between two health checks. */
    private static final Duration HEALTH_POLL_INTERVAL = Duration.ofMillis(500);

    /** Properties that point the emulator's two stores to the session's directory. */
    private static final String SESSIONS_PATH_PROPERTY = "api.emulator.storage.sessions.path";
    private static final String MOCKS_PATH_PROPERTY = "api.emulator.storage.mocks.path";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Path storageRoot;
    private final Duration startupTimeout;
    private final Function<Map<String, String>, ApiEmulatorRunner> runnerFactory;
    private final Function<String, IRestClientApi> clientFactory;

    /**
     * Creates the manager used by the test context.
     *
     * @param objectMapper mapper for request and response bodies
     * @param restTemplate template for the health checks
     * @param storageRoot  directory under which every session gets a directory of its own
     */
    @Autowired
    public EmulatorLifecycleManager(ObjectMapper objectMapper,
                                    RestTemplate restTemplate,
                                    @Value("${test-framework.storage-root:target/test-data}") String storageRoot) {
        this(objectMapper, restTemplate, Path.of(storageRoot), STARTUP_TIMEOUT,
                properties -> new ApiEmulatorRunner(0, properties),
                RestClientApiEngine::new);
    }

    /**
     * Creates a manager with explicit collaborators; used by unit tests.
     *
     * @param objectMapper   mapper for request and response bodies
     * @param restTemplate   template for the health checks
     * @param storageRoot    directory under which every session gets a directory of its own
     * @param startupTimeout how long a new emulator may take to become healthy
     * @param runnerFactory  creates a runner from the startup properties of one emulator
     * @param clientFactory  creates a client bound to the base URL of one emulator
     */
    public EmulatorLifecycleManager(ObjectMapper objectMapper,
                                    RestTemplate restTemplate,
                                    Path storageRoot,
                                    Duration startupTimeout,
                                    Function<Map<String, String>, ApiEmulatorRunner> runnerFactory,
                                    Function<String, IRestClientApi> clientFactory) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.storageRoot = storageRoot;
        this.startupTimeout = startupTimeout;
        this.runnerFactory = runnerFactory;
        this.clientFactory = clientFactory;
    }

    /**
     * Starts a new emulator and logs in to it.
     *
     * @param authRequest credentials of the user the session logs in as
     * @return a ready session; the caller must close it
     * @throws IllegalStateException if the emulator does not become healthy in time
     *                               or does not return a session token
     */
    public EmulatorSession start(AuthRequest authRequest) {
        Path storageDirectory = storageRoot.resolve(UUID.randomUUID().toString());
        ApiEmulatorRunner runner = runnerFactory.apply(storageProperties(storageDirectory));
        runner.start();
        try {
            String url = "http://localhost:" + runner.getActualPort();
            log.info(MANAGER_READY_ON_PORT, runner.getActualPort());

            IRestClientApi restClient = clientFactory.apply(url);
            waitUntilReady(url);
            String authToken = performLogin(restClient, authRequest);

            log.info(SESSION_STARTED, url);
            return new EmulatorSession(url, authToken, restClient, runner, objectMapper, storageDirectory);
        } catch (RuntimeException e) {
            runner.stop();
            throw e;
        }
    }

    /**
     * Points both stores of one emulator to the given directory.
     *
     * @param storageDirectory the session's own directory
     * @return startup properties for the emulator
     */
    private static Map<String, String> storageProperties(Path storageDirectory) {
        return Map.of(
                SESSIONS_PATH_PROPERTY, storageDirectory.resolve(EmulatorStorageProperties.DEFAULT_SESSIONS_PATH).toString(),
                MOCKS_PATH_PROPERTY, storageDirectory.resolve(EmulatorStorageProperties.DEFAULT_MOCKS_PATH).toString());
    }

    /**
     * Logs in to a freshly started emulator.
     *
     * @param restClient  client bound to the emulator
     * @param authRequest user credentials
     * @return the session token
     * @throws IllegalStateException if the response carries no token or cannot be read
     */
    private String performLogin(IRestClientApi restClient, AuthRequest authRequest) {
        JsonNode responseNode = restClient.post(URL_TOKEN, objectMapper.valueToTree(authRequest), Map.of());

        AuthResponse response;
        try {
            response = objectMapper.treeToValue(responseNode, AuthResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(FAILED_TO_READ_AUTH_RESPONSE, e);
        }

        if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || response.getData() == null) {
            throw new IllegalStateException(FAILED_TO_GENERATE_AUTH_TOKEN);
        }
        log.info(MANAGER_AUTHENTICATED_AS, authRequest.getLogin());
        return response.getData().getToken();
    }

    /**
     * Polls the Actuator health endpoint until it answers with HTTP 200.
     * <p>Checks every {@code 500 ms} until the startup timeout is reached.</p>
     *
     * @param url base URL of the emulator
     * @throws IllegalStateException if the emulator is not healthy within the timeout
     *                               or the waiting thread is interrupted
     */
    private void waitUntilReady(String url) {
        Instant deadline = Instant.now().plus(startupTimeout);
        String healthUrl = url + "/actuator/health";

        while (Instant.now().isBefore(deadline)) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info(EMULATOR_IS_READY);
                    return;
                }
            } catch (RestClientException ignored) {
                // Not accepting connections yet, poll again
            }

            try {
                Thread.sleep(HEALTH_POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(INTERRUPTED_EMULATOR_STARTUP, e);
            }
        }
        throw new IllegalStateException(FAILED_TO_START_EMULATOR + startupTimeout.toSeconds() + " seconds");
    }
}
