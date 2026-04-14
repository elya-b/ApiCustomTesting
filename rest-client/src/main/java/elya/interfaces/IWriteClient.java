package elya.interfaces;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Interface for data modification API operations.
 */
public interface IWriteClient {
    /**
     * Executes a POST request with a JSON payload.
     *
     * @param urlPath  endpoint path or full URL
     * @param jsonBody payload to be sent in the request body
     * @param headers  map of request headers
     * @return         response body as a JsonNode
     */
    JsonNode post(String urlPath, JsonNode jsonBody, Map<String, String> headers);
}
