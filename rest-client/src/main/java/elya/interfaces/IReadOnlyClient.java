package elya.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Interface defining read-only API operations.
 * <p>Used to segregate components that only require data retrieval capabilities,
 * preventing accidental state-changing operations.</p>
 */
public interface IReadOnlyClient {

    /**
     * Executes a GET request to the specified endpoint.
     *
     * @param urlPath the target endpoint path or a fully qualified URL.
     * @param headers a map containing HTTP headers (e.g., Authorization).
     * @return the response body parsed as a {@link JsonNode}.
     */
    JsonNode get(String urlPath, Map<String, String> headers);
}