package elya.restclient.http;

import java.util.*;

/**
 * An HTTP response exactly as the server sent it: status code, raw body and headers.
 * <p>Created only for a response that was actually received, so the status code is always
 * the real one, including codes outside the standard set (e.g. 520). A request that got no
 * response fails with {@link elya.restclient.exceptions.ApiTransportException} instead.</p>
 *
 * @param statusCode the HTTP status code
 * @param body       the raw response body, possibly empty
 * @param headers    the response headers; the values of a repeated header are joined with {@code "; "}
 */
public record RestClientApiResponse(int statusCode, String body, Map<String, String> headers) {

    /**
     * Makes the headers unmodifiable, so the response cannot change after it was received.
     */
    public RestClientApiResponse {
        headers = Map.copyOf(headers);
    }

    /**
     * Determines if the request was successful based on the HTTP status code.
     *
     * @return {@code true} if the status code is within the 200-299 range; {@code false} otherwise.
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}