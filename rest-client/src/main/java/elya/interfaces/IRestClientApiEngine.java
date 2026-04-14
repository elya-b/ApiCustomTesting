package elya.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import elya.restclient.objects.response.RestClientApiResponse;
import org.springframework.http.HttpMethod;

import java.util.Map;

/**
 * Interface for the core execution engine of the REST client.
 * Provides a low-level method for sending any type of HTTP request.
 */
public interface IRestClientApiEngine {
    /**
     * Sends an HTTP request and returns a comprehensive response object.
     *
     * @param method   HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param urlPath  endpoint path or full URL
     * @param jsonBody request body as JsonNode
     * @param headers  map of request headers
     * @return         a RestClientApiResponse containing status code, headers, and body
     */
    RestClientApiResponse sendRequest(
            HttpMethod method,
            String urlPath,
            JsonNode jsonBody,
            Map<String, String> headers
    );
}
