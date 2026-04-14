package elya.interfaces;


import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
/**
 * Interface for read-only API operations.
 */
public interface IReadOnlyClient {
    /**
     * Executes a GET request to the specified path.
     *
     * @param urlPath endpoint path or full URL
     * @param headers map of request headers
     * @return        response body as a JsonNode
     */
    JsonNode get(String urlPath, Map<String, String> headers);
}
