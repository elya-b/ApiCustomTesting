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
 * Handles the logic for retrieving security tokens and managing user sessions
 * via the underlying REST infrastructure.
 */
@Component
public class AuthClient implements IAuthApi {

    private final IRestClientApi clientApi;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes the AuthClient with a REST API execution engine.
     *
     * @param clientApi the low-level API client used for executing HTTP requests
     */
    public AuthClient(IRestClientApi clientApi) {
        this.clientApi = clientApi;
    }

    /**
     * Generates an authentication token for the given credentials.
     * Encapsulates the process of building the request body and mapping the response.
     *
     * @param login    user identification string
     * @param password user password string
     * @return         AuthResponse containing the generated token and session data
     * @throws RestClientException if the response content is missing or invalid
     */
    public AuthResponse generateAuthToken(AuthRequest request) {
        JsonNode body = objectMapper.valueToTree(request);
        JsonNode responseJson = clientApi.post(URL_TOKEN, body, Collections.emptyMap());

        if (RestClientApiHelper.hasContent(responseJson)) {
            return objectMapper.convertValue(responseJson, AuthResponse.class);
        }

        throw new RestClientException(GENERATE_TOKEN_EXCEPTION);
    }
}
