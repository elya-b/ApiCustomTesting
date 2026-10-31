package elya.restclient.http;

import com.fasterxml.jackson.databind.*;
import elya.restclient.exceptions.*;

import java.util.*;

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
     * @return the response body parsed as a {@link JsonNode}; an empty object for an empty body.
     * @throws ApiHttpStatusException if the server answered with a non-2xx status.
     * @throws ApiTransportException  if no HTTP response was received.
     * @throws RestClientException    if the body of a successful response is not valid JSON.
     */
    JsonNode get(String urlPath, Map<String, String> headers);
}