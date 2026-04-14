package elya.restclient.constants.logs;

/**
 * Constants for high-level exception messages.
 * Used to provide clear, categorized error descriptions (Client, Data, Security)
 * when throwing RestClientException.
 */
public class ExceptionMessage {
    /** Errors related to authentication and token generation. */
    public static final String GENERATE_TOKEN_EXCEPTION = "Client Error: Unable to generate authentication token.";

    /** Errors occurring during bank card data retrieval. */
    public static final String GET_CARDS_EXCEPTION = "Client Error: Failed to retrieve bank cards from the server.";

    /** Errors related to empty or malformed card data responses. */
    public static final String PARSE_CARDS_EXCEPTION = "Data Error: Bank cards response is empty or has an invalid format.";

    /** Errors occurring when the JSON structure does not match the DTO model. */
    public static final String UNEXPECTED_JSON_EXCEPTION = "Data Error: The server returned a JSON structure that doesn't match the expected model.";

    /** Security-related errors for token validation. */
    public static final String AUTH_TOKEN_VALIDATION_FAILED = "Security Error: Provided Bearer token is invalid or expired.";
}
