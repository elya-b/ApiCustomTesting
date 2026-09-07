package elya.constants.enums;

import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * Enumeration of standard HTTP header values and prefixes used in API communication.
 * Provides formatted strings for headers such as Authorization and Content-Type to ensure
 * consistency across the emulator and testing framework.
 */
@AllArgsConstructor
public enum HttpHeaderValues {
    /** * Prefix for Bearer authentication tokens.
     * Includes a trailing space for direct concatenation with the token string.
     */
    BEARER ("Bearer "),

    /** * Media type for JSON payloads, used primarily in Content-Type and Accept headers.
     */
    APPLICATION_JSON ("application/json");

    private final String httpHeaderValue;

    @Override
    public String toString() {
        return httpHeaderValue;
    }
}