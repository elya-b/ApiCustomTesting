package elya.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Interface defining data modification and creation API operations.
 * <p>Provides the contract for executing POST requests, enabling the
 * transmission of JSON payloads to update or create remote resources.</p>
 */
public interface IWriteClient {

    /**
     * Executes a POST request with a specific JSON payload and headers.
     *
     * @param urlPath  the target endpoint path or a fully qualified URL.
     * @param jsonBody the {@link JsonNode} payload to be sent in the request body.
     * @param headers  a map containing HTTP headers (e.g., Content-Type, Authorization).
     * @return the response body parsed as a {@link JsonNode}.
     */
    JsonNode post(String urlPath, JsonNode jsonBody, Map<String, String> headers);
}