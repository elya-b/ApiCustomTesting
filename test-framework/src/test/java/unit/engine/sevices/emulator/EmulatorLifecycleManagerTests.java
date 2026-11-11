package unit.engine.sevices.emulator;

import com.fasterxml.jackson.databind.*;
import elya.dto.auth.*;
import elya.emulator.*;
import elya.engine.services.emulator.*;
import elya.restclient.exceptions.*;
import elya.restclient.http.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.io.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.http.*;
import org.springframework.web.client.*;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

import static elya.constants.ApiEndpoints.*;
import static elya.engine.services.emulator.constants.exceptions.Exceptions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (Mockito) for {@link EmulatorLifecycleManager}.
 * <ul>
 *   <li>{@code start()} returns a session with the emulator URL and the session token</li>
 *   <li>{@code start()} gives every session its own storage directory under the storage root</li>
 *   <li>{@code start()} waits until the emulator is healthy (second health check succeeds)</li>
 *   <li>{@code start()} stops the emulator when it is not healthy within the timeout</li>
 *   <li>{@code start()} stops the emulator when the login is rejected with an HTTP error</li>
 *   <li>{@code start()} stops the emulator when the token response is invalid (parameterized)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EmulatorLifecycleManagerTests {

    private static final int PORT = 54321;
    private static final String URL = "http://localhost:" + PORT;
    private static final String TOKEN = "token";
    private static final AuthRequest LOGIN_REQUEST = new AuthRequest("admin_user", "admin_password");
    private static final String SESSIONS_PATH_PROPERTY = "api.emulator.storage.sessions.path";
    private static final String MOCKS_PATH_PROPERTY = "api.emulator.storage.mocks.path";

    @TempDir
    Path storageRoot;

    @Mock
    private ApiEmulatorRunner runner;
    @Mock
    private IRestClientApi restClient;
    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Map<String, String>> runnerProperties = new ArrayList<>();
    private final List<String> clientUrls = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(runner.getActualPort()).thenReturn(PORT);
    }

    @Test
    @DisplayName("start() - Should return a session with the emulator URL and the session token")
    void start_ShouldReturnSession_WhenEmulatorIsHealthyAndLoginSucceeds() {
        givenHealthyEmulator();
        givenLoginResponse(AuthResponse.success(TOKEN, 3600, Instant.EPOCH, "issuer"));

        EmulatorSession session = manager(Duration.ofSeconds(2)).start(LOGIN_REQUEST);

        assertAll("Verify the started session",
                () -> assertEquals(URL, session.getUrl(), "URL must point to the emulator port"),
                () -> assertEquals(TOKEN, session.getAuthToken(), "Token must come from the login response"),
                () -> assertNotNull(session.getClientApi(), "Session must have its own client"),
                () -> assertEquals(List.of(URL), clientUrls, "Client must be bound to the emulator URL"));
        verify(runner).start();
        verify(runner, never()).stop();
    }

    @Test
    @DisplayName("start() - Should give every session its own storage directory")
    void start_ShouldGiveEachSessionItsOwnStorageDirectory() {
        givenHealthyEmulator();
        givenLoginResponse(AuthResponse.success(TOKEN, 3600, Instant.EPOCH, "issuer"));
        EmulatorLifecycleManager manager = manager(Duration.ofSeconds(2));

        manager.start(LOGIN_REQUEST);
        manager.start(LOGIN_REQUEST);

        Path firstDirectory = Path.of(runnerProperties.get(0).get(SESSIONS_PATH_PROPERTY)).getParent();
        Path secondDirectory = Path.of(runnerProperties.get(1).get(SESSIONS_PATH_PROPERTY)).getParent();
        assertAll("Verify the storage directories",
                () -> assertNotEquals(firstDirectory, secondDirectory, "Sessions must not share a directory"),
                () -> assertEquals(storageRoot, firstDirectory.getParent(), "Directory must be under the storage root"),
                () -> assertEquals(firstDirectory,
                        Path.of(runnerProperties.get(0).get(MOCKS_PATH_PROPERTY)).getParent(),
                        "Both stores of one emulator must use the same directory"));
    }

    @Test
    @DisplayName("start() - Should wait until the emulator is healthy")
    void start_ShouldWaitUntilHealthy_WhenFirstHealthCheckFails() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        givenLoginResponse(AuthResponse.success(TOKEN, 3600, Instant.EPOCH, "issuer"));

        assertDoesNotThrow(() -> manager(Duration.ofSeconds(2)).start(LOGIN_REQUEST));

        verify(restTemplate, times(2)).getForEntity(URL + "/actuator/health", String.class);
    }

    @Test
    @DisplayName("start() - Should stop the emulator when it is not healthy within the timeout")
    void start_ShouldStopEmulator_WhenHealthCheckTimesOut() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        var exception = assertThrows(IllegalStateException.class,
                () -> manager(Duration.ofMillis(100)).start(LOGIN_REQUEST));

        assertTrue(exception.getMessage().startsWith(FAILED_TO_START_EMULATOR),
                "Expected the timeout message, got: " + exception.getMessage());
        verify(runner).stop();
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("start() - Should stop the emulator when the login is rejected")
    void start_ShouldStopEmulator_WhenLoginIsRejected() {
        givenHealthyEmulator();
        when(restClient.post(eq(URL_TOKEN), any(), anyMap()))
                .thenThrow(new ApiHttpStatusException(401, "Invalid or missing credentials."));

        var exception = assertThrows(ApiHttpStatusException.class,
                () -> manager(Duration.ofSeconds(2)).start(LOGIN_REQUEST));

        assertEquals(401, exception.getStatusCode(), "The HTTP status must reach the caller unchanged");
        verify(runner).stop();
    }

    @ParameterizedTest
    @MethodSource("invalidAuthResponses")
    @DisplayName("start() - Should stop the emulator when the token response is invalid")
    void start_ShouldStopEmulator_WhenTokenResponseIsInvalid(AuthResponse invalidResponse) {
        givenHealthyEmulator();
        givenLoginResponse(invalidResponse);

        var exception = assertThrows(IllegalStateException.class,
                () -> manager(Duration.ofSeconds(2)).start(LOGIN_REQUEST));

        assertEquals(FAILED_TO_GENERATE_AUTH_TOKEN, exception.getMessage(), "Exception message mismatch");
        verify(runner).stop();
    }

    private static Stream<Arguments> invalidAuthResponses() {
        return Stream.of(
                Arguments.of(AuthResponse.builder()
                        .success(false)
                        .data(AuthResponseData.builder().token(TOKEN).build())
                        .build()),
                Arguments.of(AuthResponse.builder()
                        .success(true)
                        .data(null)
                        .build())
        );
    }

    private EmulatorLifecycleManager manager(Duration startupTimeout) {
        return new EmulatorLifecycleManager(objectMapper, restTemplate, storageRoot, startupTimeout,
                properties -> {
                    runnerProperties.add(properties);
                    return runner;
                },
                url -> {
                    clientUrls.add(url);
                    return restClient;
                });
    }

    private void givenHealthyEmulator() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
    }

    private void givenLoginResponse(AuthResponse response) {
        JsonNode body = objectMapper.valueToTree(response);
        when(restClient.post(eq(URL_TOKEN), any(), anyMap())).thenReturn(body);
    }
}
