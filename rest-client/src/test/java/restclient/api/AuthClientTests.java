package restclient.api;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import elya.dto.auth.*;
import elya.restclient.api.*;
import elya.restclient.exceptions.*;
import elya.restclient.http.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.time.*;

import static elya.constants.ApiEndpoints.*;
import static elya.constants.enums.JsonProperty.*;
import static elya.enums.responsemodel.AuthToken.*;
import static elya.restclient.constants.messages.ExceptionMessages.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (Mockito) for {@link AuthClient}.
 * <ul>
 *   <li>{@code generateAuthToken()} — returns AuthResponse for valid credentials</li>
 *   <li>{@code generateAuthToken()} — throws RestClientException when response is null</li>
 *   <li>{@code generateAuthToken()} — throws RestClientException when response is an empty object</li>
 *   <li>{@code generateAuthToken()} — sends the correct request body (login + password)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class AuthClientTests {

    @Mock
    private IRestClientApi clientApi;

    @InjectMocks
    private AuthClient authClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final AuthRequest AUTH_REQUEST = new AuthRequest("test","test");

    @Test
    @DisplayName("generateAuthToken() - Should return AuthResponse when credentials are valid")
    void generateAuthToken_ShouldReturnResponse_WhenCredentialsAreValid() {

        var authResponse = AuthResponse.success("token", 3600, Instant.EPOCH, "issuer");

        when(clientApi.post(anyString(), any(), anyMap())).thenReturn(toJson(authResponse));

        var response = authClient.generateAuthToken(AUTH_REQUEST);

        assertAll("AuthResponse validation",
                () -> assertNotNull(response, "Response should not be null"),
                () -> assertTrue(response.getSuccess(), "Response status should be true"),
                () -> assertEquals(SUCCESS.toString(), response.getMessage(), "Message should match")
        );

        verify(clientApi).post(eq(URL_TOKEN), any(), anyMap());
    }

    @Test
    @DisplayName("generateAuthToken() - Should throw RestClientException when response is empty")
    void generateAuthToken_ShouldThrowException_WhenResponseIsEmpty() {
        when(clientApi.post(anyString(), any(), anyMap())).thenReturn(null);

        var exception = assertThrows(RestClientException.class, () -> {
            authClient.generateAuthToken(AUTH_REQUEST);
        });

        assertEquals(GENERATE_TOKEN_EXCEPTION, exception.getMessage());
    }

    @Test
    @DisplayName("generateAuthToken() - Should throw exception when response is empty object")
    void generateAuthToken_ShouldThrowException_WhenResponseIsEmptyObject() {
        ObjectNode emptyNode = objectMapper.createObjectNode();
        when(clientApi.post(anyString(), any(), anyMap())).thenReturn(emptyNode);

        assertThrows(RestClientException.class, () -> authClient.generateAuthToken(AUTH_REQUEST));
    }

    @Test
    @DisplayName("generateAuthToken() - Should send correct request body")
    void generateAuthToken_ShouldSendCorrectBody() {
        var fakeJsonResponse = AuthResponse.builder().success(true).build();
        when(clientApi.post(anyString(), any(), anyMap())).thenReturn(toJson(fakeJsonResponse));

        authClient.generateAuthToken(AUTH_REQUEST);

        var bodyCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(clientApi).post(eq(URL_TOKEN), bodyCaptor.capture(), anyMap());

        var capturedBody = bodyCaptor.getValue();
        assertAll("Captured body validation",
                () -> assertEquals(AUTH_REQUEST.getLogin(), capturedBody.get(LOGIN.toString()).asText()),
                () -> assertEquals(AUTH_REQUEST.getPassword(), capturedBody.get(PASSWORD.toString()).asText())
        );
    }

    /**
     * Helper method to convert objects to JsonNode for mocking clientApi responses.
     */
    private JsonNode toJson(Object obj) {
        return objectMapper.valueToTree(obj);
    }
}
