package elya.restclient.http;

import com.fasterxml.jackson.databind.*;
import elya.restclient.exceptions.*;
import org.springframework.http.*;

import java.util.*;

/**
 * Interface defining the core execution logic for the REST infrastructure.
 * <p>Serves as the low-level dispatcher responsible for transforming abstract
 * request definitions into actual network exchanges and returning the response
 * exactly as the server sent it.</p>
 */
public interface IRestClientApiEngine {

    /**
     * Dispatches an HTTP request and returns the response whatever its status.
     *
     * @param method   the {@link HttpMethod} (GET, POST, etc.) to be used.
     * @param urlPath  the target endpoint path or a fully qualified URI.
     * @param jsonBody an optional {@link JsonNode} representing the request payload.
     * @param headers  a map of HTTP headers to include in the request.
     * @return a {@link RestClientApiResponse} with the real status code, raw body and headers.
     * @throws ApiTransportException if no HTTP response was received.
     */
    RestClientApiResponse sendRequest(
            HttpMethod method,
            String urlPath,
            JsonNode jsonBody,
            Map<String, String> headers
    );
}