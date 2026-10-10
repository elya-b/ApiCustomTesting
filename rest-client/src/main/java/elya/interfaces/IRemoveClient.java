package elya.interfaces;

import elya.restclient.exceptions.ApiHttpStatusException;
import elya.restclient.exceptions.RestClientException;

import java.util.Map;

/**
 * Interface defining resource removal API operations.
 * <p>Provides specialized methods for executing DELETE requests,
 * allowing for both simple and authorized removal of remote resources.</p>
 * <p>A failed removal is reported by an exception carrying the HTTP status,
 * never by a return value: a caller must never have to guess whether a delete
 * failed because the resource was missing, the token was rejected, or the
 * server was unreachable.</p>
 */
public interface IRemoveClient {

    /**
     * Executes a standard DELETE request to the specified path.
     *
     * @param urlPath the target endpoint path or a fully qualified URL.
     * @throws ApiHttpStatusException if the server answered with a non-2xx status.
     * @throws RestClientException    if the response carries no usable HTTP status.
     */
    void delete(String urlPath);

    /**
     * Executes an authorized or metadata-enriched DELETE request.
     *
     * @param urlPath the target endpoint path or a fully qualified URL.
     * @param headers a map containing HTTP headers (e.g., security tokens).
     * @throws ApiHttpStatusException if the server answered with a non-2xx status.
     * @throws RestClientException    if the response carries no usable HTTP status.
     */
    void delete(String urlPath, Map<String, String> headers);
}