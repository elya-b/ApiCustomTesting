package elya.restclient.exceptions;

import lombok.Getter;

/**
 * Thrown when the server answered with a non-2xx status.
 * <p>Unlike {@link RestClientException}, which covers any client-side failure,
 * this type guarantees that the server did respond and that an HTTP status is
 * always present — the invariant a negative test asserts on.</p>
 */
@Getter
public class ApiHttpStatusException extends RestClientException {

    /** The HTTP status code returned by the server. */
    private final int statusCode;

    /** The raw response body, kept for assertions and diagnostics. */
    private final String responseBody;

    public ApiHttpStatusException(int statusCode, String responseBody) {
        super("Unexpected HTTP status " + statusCode + ". Body: " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

}
