package elya.enums.responsemodel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Enumeration representing key constants and status identifiers for authentication token responses.
 * <p>Used to maintain consistency in status messaging and JSON field values
 * throughout the authorization and session management lifecycle.</p>
 */
@Getter
@AllArgsConstructor
public enum AuthToken {

    /**
     * Represents a successful outcome of an authentication or token-related operation.
     * Typically used in the 'Message' field of the {@code AuthResponse}.
     */
    SUCCESS("Success");

    /**
     * The literal string value used in the API response payload.
     */
    private final String responseKey;

    @Override
    public String toString() {
        return responseKey;
    }
}