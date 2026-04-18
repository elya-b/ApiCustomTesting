package elya.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.RestClientApiHelper;
import elya.apicontracts.IAuthApi;
import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;
import elya.interfaces.IRestClientApi;
import elya.restclient.constants.logs.RestClientException;
import org.springframework.stereotype.Component;

import java.util.Collections;

import static elya.constants.ApiEndpoints.URL_TOKEN;
import static elya.restclient.constants.logs.ExceptionMessage.GENERATE_TOKEN_EXCEPTION;

/**
 * Implementation of the authentication API client.
 * <p>Provides a high-level interface for security-related operations,
 * specifically managing user sessions and token retrieval from the remote emulator.</p>
 */
@Component
public class AuthClient implements IAuthApi {

    /** The low-level execution engine for HTTP requests. */
    private final IRestClientApi clientApi;

    /** Internal mapper for JSON transformations. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes the AuthClient with the required REST engine.
     *
     * @param clientApi the {@link IRestClientApi} implementation used for networking.
     */
    public AuthClient(IRestClientApi clientApi) {
        this.clientApi = clientApi;
    }

    /**
     * Generates a security token based on the provided credentials.
     * <p>The method serializes the {@link AuthRequest}, executes a POST request to
     * the token endpoint, and maps the successful response back to {@link AuthResponse}.</p>
     *
     * @param request the authentication request object containing user credentials.
     * @return a validated {@link AuthResponse} containing the session token.
     * @throws RestClientException if the server response is empty or malformed.
     */
    @Override
    public AuthResponse generateAuthToken(AuthRequest request) {
        JsonNode body = objectMapper.valueToTree(request);
        JsonNode responseJson = clientApi.post(URL_TOKEN, body, Collections.emptyMap());

        if (RestClientApiHelper.hasContent(responseJson)) {
            return objectMapper.convertValue(responseJson, AuthResponse.class);
        }

        throw new RestClientException(GENERATE_TOKEN_EXCEPTION);
    }
}