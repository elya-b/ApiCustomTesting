package elya.constants.enums;

/**
 * Enumeration of standard HTTP header values used in API communication.
 * Provides formatted strings for headers such as Authorization and Content-Type.
 */
public enum HttpHeaderValues {
    /** Prefix for Bearer authentication tokens. */
    BEARER ("Bearer "),

    /** Media type for JSON payloads. */
    APPLICATION_JSON ("application/json");

    private final String httpHeaderValue;

    /**
     * Initializes the enum constant with its corresponding header value string.
     *
     * @param httpHeaderValue the literal string value for the HTTP header
     */
    HttpHeaderValues (String httpHeaderValue) {
        this.httpHeaderValue = httpHeaderValue;
    }

    /**
     * Returns the string representation of the HTTP header value.
     *
     * @return the header value string
     */
    @Override
    public String toString() {
        return httpHeaderValue;
    }
}
