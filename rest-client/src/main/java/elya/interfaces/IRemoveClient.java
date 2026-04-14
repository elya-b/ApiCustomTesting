package elya.interfaces;

import java.util.Map;

/**
 * Interface for resource removal API operations.
 */
public interface IRemoveClient {
    /**
     * Executes a DELETE request to the specified path.
     */
    boolean delete(String urlPath);

    /**
     * Executes a DELETE request with custom headers.
     *
     * @param urlPath endpoint path
     * @param headers HTTP headers (e.g., Authorization)
     * @return        true if successful
     */
    boolean delete(String urlPath, Map<String, String> headers);
}
