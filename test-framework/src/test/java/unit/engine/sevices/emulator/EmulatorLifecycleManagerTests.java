package unit.engine.sevices.emulator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;
import elya.dto.auth.AuthResponseData;
import elya.engine.services.emulator.EmulatorLifecycleManager;
import elya.interfaces.IRestClientApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.stream.Stream;

import static elya.engine.services.emulator.constants.exceptions.Exceptions.FAILED_TO_GENERATE_AUTH_TOKEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (Mockito) for {@link elya.engine.services.emulator.EmulatorLifecycleManager}.
 * <ul>
 *   <li>{@code performLogin()} — returns a token for a valid response</li>
 *   <li>{@code performLogin()} — throws IllegalStateException for invalid responses (parameterized: success=false or data=null)</li>
 *   <li>{@code waitUntilReady()} — succeeds when the emulator becomes ready on the second attempt</li>
 *   <li>{@code waitUntilReady()} — throws an exception after timeout when the service is unavailable</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class EmulatorLifecycleManagerTests {

    @Mock
    private IRestClientApi restClient;
    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private EmulatorLifecycleManager manager;

    private final String baseUrl = "http://localhost:8080";
    private static final String TOKEN = "token";
    private static final AuthRequest LOGIN_REQUEST = new AuthRequest("admin_user", "admin_password");

    @BeforeEach
    void setUp() {
        manager = new EmulatorLifecycleManager(restClient, objectMapper, restTemplate);
        ReflectionTestUtils.setField(manager, "url", baseUrl);
    }

    @Test
    @DisplayName("performLogin() - Should return token when response is valid")
    void performLogin_ShouldSetToken_WhenResponseIsValid() {
        var authResponse = AuthResponse.success(TOKEN, "3600", null, "issuer");

        when(restClient.post(anyString(), any(), anyMap())).thenReturn(toJson(authResponse));

        String resultToken = ReflectionTestUtils.invokeMethod(manager, "performLogin", LOGIN_REQUEST);

        assertEquals(TOKEN, resultToken, "The method should return the token from the response");
    }

    @ParameterizedTest
    @MethodSource("provideInvalidAuthResponses")
    @DisplayName("performLogin: should throw IllegalStateException for invalid responses")
    void performLogin_ShouldThrowException_WhenResponseIsInvalid(AuthResponse invalidResponse) {
        when(restClient.post(anyString(), any(), anyMap())).thenReturn(toJson(invalidResponse));

        var exception = assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(manager, "performLogin", LOGIN_REQUEST));

        assertAll("Verify failure state and message",
                () -> assertEquals(FAILED_TO_GENERATE_AUTH_TOKEN, exception.getMessage(), "Exception message mismatch"),
                () -> assertNull(manager.getAuthToken(), "Auth token should not be set on invalid response")
        );
    }

    private static Stream<Arguments> provideInvalidAuthResponses() {
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

    @Test
    @DisplayName("waitUntilReady() - Should succeed when emulator becomes ready on second attempt")
    void waitUntilReady_ShouldSucceed_OnSecondAttempt() {
        ResponseEntity<String> successResponse = new ResponseEntity<>(HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Server not ready"))
                .thenReturn(successResponse);

        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(manager, "waitUntilReady", Duration.ofSeconds(2))
        );

        verify(restTemplate, times(2)).getForEntity(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("waitUntilReady() - Should throw exception after timeout when service is unhealthy")
    void waitUntilReady_ShouldThrowException_WhenTimeoutReached() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        Duration shortTimeout = Duration.ofMillis(100);
        var exception = assertThrows(RuntimeException.class, () ->
                ReflectionTestUtils.invokeMethod(manager, "waitUntilReady", shortTimeout)
        );

        String expectedPrefix = "Emulator failed to start within ";
        assertTrue(exception.getMessage().contains(expectedPrefix),
                "Expected timeout message but got: " + exception.getMessage());

        verify(restTemplate, atLeastOnce()).getForEntity(anyString(), eq(String.class));
    }

    private JsonNode toJson(Object obj) {
        return objectMapper.valueToTree(obj);
    }
}