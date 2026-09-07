package elya.interfaces;

import java.util.Map;

/**
 * Interface defining resource removal API operations.
 * <p>Provides specialized methods for executing DELETE requests,
 * allowing for both simple and authorized removal of remote resources.</p>
 */
public interface IRemoveClient {

    /**
     * Executes a standard DELETE request to the specified path.
     *
     * @param urlPath the target endpoint path or a fully qualified URL.
     * @return {@code true} if the resource was successfully removed; {@code false} otherwise.
     */
    boolean delete(String urlPath);

    /**
     * Executes an authorized or metadata-enriched DELETE request.
     *
     * @param urlPath the target endpoint path or a fully qualified URL.
     * @param headers a map containing HTTP headers (e.g., security tokens).
     * @return {@code true} if the operation was successful; {@code false} otherwise.
     */
    boolean delete(String urlPath, Map<String, String> headers);
}