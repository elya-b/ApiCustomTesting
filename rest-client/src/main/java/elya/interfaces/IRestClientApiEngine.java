package elya.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import elya.restclient.objects.response.RestClientApiResponse;
import org.springframework.http.HttpMethod;

import java.util.Map;

/**
 * Interface defining the core execution logic for the REST infrastructure.
 * <p>Serves as the low-level dispatcher responsible for transforming abstract
 * request definitions into actual network exchanges and capturing raw results
 * into a standardized response container.</p>
 */
public interface IRestClientApiEngine {

    /**
     * Dispatches an HTTP request and captures comprehensive execution metadata.
     *
     * @param method   the {@link HttpMethod} (GET, POST, etc.) to be used.
     * @param urlPath  the target endpoint path or a fully qualified URI.
     * @param jsonBody an optional {@link JsonNode} representing the request payload.
     * @param headers  a map of HTTP headers to include in the request.
     * @return a {@link RestClientApiResponse} containing the status code,
     * processed headers, and the response body in multiple formats.
     */
    RestClientApiResponse sendRequest(
            HttpMethod method,
            String urlPath,
            JsonNode jsonBody,
            Map<String, String> headers
    );
}